#!/usr/bin/env bash
set -euo pipefail

escape_yaml_path() {
  echo "$1" | sed 's/\\/\\\\/g'
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[smoke-invalid-rule] Building agent..."
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
run_root="/tmp/mja-smoke/invalid-rule"
timestamp="$(date +%Y%m%d_%H%M%S)"
run_dir="$run_root/$timestamp"
target_classes="$run_dir/target-classes"
trace_root="$run_dir/trace"
agent_log_dir="$run_dir/agent-logs"
config_file="$run_dir/config.yaml"
agent_jar_local="$run_dir/agent.jar"

mkdir -p "$target_classes" "$run_dir" "$trace_root" "$agent_log_dir"
cp "$agent_jar" "$agent_jar_local"

echo "[smoke-invalid-rule] Compiling target app..."
javac -d "$target_classes" "$target_src"

escaped_trace_root="$(escape_yaml_path "$trace_root")"
cat > "$config_file" <<EOF
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$escaped_trace_root"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::NOT_A_REAL_ACTION
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: false
printJVMCpuUsage: false
printJVMThreadUsage: false
printJVMGCStats: false
printJVMClassLoaderStats: false
exposeMetrics: false
metricsPort: 0
maxHeapDumps: 0
sendAlertEmails: false
emailRecipientList: []
EOF

agent_args="configFile=$config_file,agentLogFileDir=$agent_log_dir,agentLogLevel=INFO,agentJarPath=$agent_jar_local"
target_stdout="$run_dir/target.out.log"
target_stderr="$run_dir/target.err.log"

echo "[smoke-invalid-rule] Starting target app with invalid rule config..."
set +e
java -Xverify:none "-javaagent:$agent_jar_local=$agent_args" -cp "$target_classes" com.monarchit.target.TargetApp >"$target_stdout" 2>"$target_stderr"
exit_code=$?
set -e

stderr_text="$(cat "$target_stderr" 2>/dev/null || true)"
agent_log_file="$agent_log_dir/monarchAgent.log"
agent_log=""
if [[ -f "$agent_log_file" ]]; then
  agent_log="$(cat "$agent_log_file")"
fi

if [[ "$exit_code" -eq 0 && -z "$stderr_text" && "$agent_log" != *"Exiting Monarch Java Agent"* ]]; then
  echo "Invalid rule did not cause expected startup failure" >&2
  exit 1
fi

echo "[smoke-invalid-rule] PASS"
echo "[smoke-invalid-rule] Run dir: $run_dir"
