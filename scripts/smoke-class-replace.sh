#!/usr/bin/env bash
set -euo pipefail

escape_yaml_path() {
  echo "$1" | sed 's/\\/\\\\/g'
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[smoke-class-replace] Building agent..."
(
  cd "$repo_root"
  mvn -q -DskipTests package
)

agent_jar="$(find "$repo_root/target" -maxdepth 1 -type f -name "*.jar" ! -name "original-*" | head -n 1)"
if [[ -z "${agent_jar:-}" ]]; then
  echo "No built agent jar found under target/" >&2
  exit 1
fi

target_src="$repo_root/it/target-app/src/com/asm/mja/it/TargetApp.java"
run_root="/tmp/mja-smoke/class-replace"
timestamp="$(date +%Y%m%d_%H%M%S)"
run_dir="$run_root/$timestamp"
target_classes="$run_dir/target-classes"
patch_src="$run_dir/patch-src/TargetApp.java"
patch_classes="$run_dir/patch-classes"
patch_jar="$run_dir/patches.jar"
invalid_class="$run_dir/invalid.class"
trace_root="$run_dir/trace"
agent_log_dir="$run_dir/agent-logs"
config_file="$run_dir/config.yaml"
agent_jar_local="$run_dir/agent.jar"

mkdir -p "$target_classes" "$patch_classes" "$(dirname "$patch_src")" "$run_dir" "$trace_root" "$agent_log_dir"
cp "$agent_jar" "$agent_jar_local"

echo "[smoke-class-replace] Compiling target app..."
javac -d "$target_classes" "$target_src"

cp "$target_src" "$patch_src"
sed -i.bak 's/return a + b + (appSeed - appSeed);/return 4242;/' "$patch_src"
rm -f "$patch_src.bak"

echo "[smoke-class-replace] Compiling replacement class and jar..."
javac -d "$patch_classes" "$patch_src"
patch_class_file="$patch_classes/com/monarchit/target/TargetApp.class"
jar cf "$patch_jar" -C "$patch_classes" com/monarchit/target/TargetApp.class
printf 'not-a-valid-class-file' > "$invalid_class"

escaped_trace_root="$(escape_yaml_path "$trace_root")"
escaped_patch_class="$(escape_yaml_path "$patch_class_file")"
escaped_patch_jar="$(escape_yaml_path "$patch_jar")"
escaped_invalid_class="$(escape_yaml_path "$invalid_class")"
cat > "$config_file" <<EOF
mode: instrumenter
instrumentation:
  enabled: true
  configRefreshInterval: 1000
  traceFileLocation: "$escaped_trace_root"
  agentRules:
    - com.monarchit.target.TargetApp@CHANGE::FILE::[$escaped_patch_class]
    - com.monarchit.target.*@CHANGE::JAR::[$escaped_patch_jar]
    - com.monarchit.target.MissingClass@CHANGE::FILE::[$escaped_patch_class]
    - com.monarchit.target.TargetApp@CHANGE::FILE::[$escaped_invalid_class]
observer:
  enabled: false
  printClassLoaderTrace: false
  printJVMSystemProperties: false
  printEnvironmentVariables: false
  metrics:
    exposeHttp: false
    port: 0
    heapUsage: false
    cpuUsage: false
    threadUsage: false
    gcStats: false
    classLoaderStats: false
alerts:
  enabled: false
  maxHeapDumps: 1
  emailRecipientList: []
EOF

target_stdout="$run_dir/target.out.log"
target_stderr="$run_dir/target.err.log"

echo "[smoke-class-replace] Starting target app without agent..."
java -Xverify:none -cp "$target_classes" com.monarchit.target.TargetApp >"$target_stdout" 2>"$target_stderr" &
target_pid=$!

cleanup() {
  if kill -0 "$target_pid" >/dev/null 2>&1; then
    kill -9 "$target_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

sleep 3

echo "[smoke-class-replace] Attaching agent to PID $target_pid..."
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set. Set JAVA_HOME to a JDK installation for attach mode." >&2
  exit 1
fi

java_bin="$JAVA_HOME/bin/java"
attach_cp="$agent_jar_local"
if [[ -f "$JAVA_HOME/lib/tools.jar" ]]; then
  attach_cp="$agent_jar_local:$JAVA_HOME/lib/tools.jar"
fi

"$java_bin" -cp "$attach_cp" com.asm.mja.attach.AgentAttachCLI \
  -agentJar "$agent_jar_local" \
  -configFile "$config_file" \
  -args "agentLogFileDir=$agent_log_dir,agentLogLevel=INFO" \
  -pid "$target_pid"

sleep 8

trace_dir="$(find "$trace_root" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [[ -z "${trace_dir:-}" ]]; then
  echo "No trace directory created under $trace_root after attach" >&2
  exit 1
fi

trace_file="$trace_dir/agent.trace"
if [[ ! -f "$trace_file" ]]; then
  echo "Trace file not found: $trace_file" >&2
  exit 1
fi

trace_text="$(cat "$trace_file")"
grep -q "Class replacement requested for com.monarchit.target.TargetApp using FILE source" <<<"$trace_text"
grep -q "Class replacement succeeded for com.monarchit.target.TargetApp" <<<"$trace_text"
grep -q "Class replacement requested for com.monarchit.target.TargetApp using JAR source" <<<"$trace_text"
grep -q "Class replacement skipped; no loaded class matched pattern com.monarchit.target.MissingClass" <<<"$trace_text"
grep -q "Class replacement failed for com.monarchit.target.TargetApp from $escaped_invalid_class" <<<"$trace_text"

backup_file="$trace_dir/backup/com_monarchit_target_TargetApp.class"
if [[ ! -f "$backup_file" ]]; then
  echo "Expected backup class was not created: $backup_file" >&2
  exit 1
fi

echo "[smoke-class-replace] PASS"
echo "[smoke-class-replace] Trace: $trace_file"
echo "[smoke-class-replace] Run dir: $run_dir"
