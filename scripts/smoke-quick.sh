#!/usr/bin/env bash
set -euo pipefail

escape_yaml_path() {
  echo "$1" | sed 's/\\/\\\\/g'
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[smoke-quick] Building agent..."
(
  cd "$repo_root"
  mvn -q -DskipTests clean package
)

agent_jar="$(find "$repo_root/target" -maxdepth 1 -type f -name "*.jar" ! -name "original-*" | head -n 1)"
if [[ -z "${agent_jar:-}" ]]; then
  echo "No built agent jar found under target/" >&2
  exit 1
fi

target_src="$repo_root/it/target-app/src/com/asm/mja/it/TargetApp.java"
run_root="/tmp/mja-smoke/quick"
timestamp="$(date +%Y%m%d_%H%M%S)"
run_dir="$run_root/$timestamp"
target_classes="$run_dir/target-classes"
trace_root="$run_dir/trace"
agent_log_dir="$run_dir/agent-logs"
config_file="$run_dir/config.yaml"
agent_jar_local="$run_dir/agent.jar"

mkdir -p "$target_classes" "$run_dir" "$trace_root" "$agent_log_dir"
cp "$agent_jar" "$agent_jar_local"

echo "[smoke-quick] Compiling target app..."
javac -d "$target_classes" "$target_src"

escaped_trace_root="$(escape_yaml_path "$trace_root")"
cat > "$config_file" <<EOF
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$escaped_trace_root"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ARGS
  - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: true
printJVMCpuUsage: true
printJVMThreadUsage: true
printJVMGCStats: true
printJVMClassLoaderStats: true
exposeMetrics: false
metricsPort: 0
maxHeapDumps: 0
sendAlertEmails: false
emailRecipientList: []
EOF

agent_args="configFile=$config_file,agentLogFileDir=$agent_log_dir,agentLogLevel=INFO,agentJarPath=$agent_jar_local"
target_stdout="$run_dir/target.out.log"
target_stderr="$run_dir/target.err.log"

echo "[smoke-quick] Starting target app with -javaagent..."
java -Xverify:none "-javaagent:$agent_jar_local=$agent_args" -cp "$target_classes" com.monarchit.target.TargetApp >"$target_stdout" 2>"$target_stderr" &
target_pid=$!

cleanup() {
  if kill -0 "$target_pid" >/dev/null 2>&1; then
    kill -9 "$target_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

sleep 8

trace_dir="$(find "$trace_root" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [[ -z "${trace_dir:-}" ]]; then
  echo "No trace directory created under $trace_root" >&2
  exit 1
fi
trace_file="$trace_dir/agent.trace"
if [[ ! -f "$trace_file" ]]; then
  echo "Trace file not found: $trace_file" >&2
  exit 1
fi

trace_text="$(cat "$trace_file")"
grep -q "ARGS |" <<<"$trace_text"
grep -q "RET |" <<<"$trace_text"
grep -q "Current JVM CPU Load" <<<"$trace_text"
grep -q "GC Stats -" <<<"$trace_text"
grep -q "Thread Stats -" <<<"$trace_text"
grep -q "ClassLoader Stats -" <<<"$trace_text"
grep -q "{USED:" <<<"$trace_text"

echo "[smoke-quick] PASS"
echo "[smoke-quick] Trace: $trace_file"
echo "[smoke-quick] Run dir: $run_dir"
