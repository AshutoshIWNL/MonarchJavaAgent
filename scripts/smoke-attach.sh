#!/usr/bin/env bash
set -euo pipefail

escape_yaml_path() {
  echo "$1" | sed 's/\\/\\\\/g'
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[smoke-attach] Building agent..."
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
run_root="/tmp/mja-smoke/attach"
timestamp="$(date +%Y%m%d_%H%M%S)"
run_dir="$run_root/$timestamp"
target_classes="$run_dir/target-classes"
trace_root="$run_dir/trace"
agent_log_dir="$run_dir/agent-logs"
config_file="$run_dir/config.yaml"
agent_jar_local="$run_dir/agent.jar"
metrics_port=9100

mkdir -p "$target_classes" "$run_dir" "$trace_root" "$agent_log_dir"
cp "$agent_jar" "$agent_jar_local"

echo "[smoke-attach] Compiling target app..."
javac -d "$target_classes" "$target_src"

codepoint_line="$(grep -n "CODEPOINT_TARGET" "$target_src" | head -n 1 | cut -d: -f1)"
if [[ -z "${codepoint_line:-}" ]]; then
  echo "Could not find CODEPOINT_TARGET marker in target app source" >&2
  exit 1
fi

escaped_trace_root="$(escape_yaml_path "$trace_root")"
cat > "$config_file" <<EOF
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$escaped_trace_root"
agentRules:
  - com.monarchit.target.TargetApp::TargetApp@INGRESS::ARGS
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ARGS
  - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::STACK
  - com.monarchit.target.TargetApp::filteredStackMethod@INGRESS::STACK::[com.monarchit.target.TargetApp.main]
  - com.monarchit.target.TargetApp::profileWork@PROFILE
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("ADD_MARKER");]
  - com.monarchit.target.TargetApp::lineProbe@CODEPOINT($codepoint_line)::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("CODEPOINT_MARKER");]
  - com.monarchit.target.TargetApp::memoryBurst@INGRESS::HEAP
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: true
printJVMCpuUsage: true
printJVMThreadUsage: true
printJVMGCStats: true
printJVMClassLoaderStats: true
exposeMetrics: true
metricsPort: $metrics_port
maxHeapDumps: 1
sendAlertEmails: false
emailRecipientList: []
EOF

target_stdout="$run_dir/target.out.log"
target_stderr="$run_dir/target.err.log"

echo "[smoke-attach] Starting target app without agent..."
java -Xverify:none -cp "$target_classes" com.monarchit.target.TargetApp >"$target_stdout" 2>"$target_stderr" &
target_pid=$!

cleanup() {
  if kill -0 "$target_pid" >/dev/null 2>&1; then
    kill -9 "$target_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

sleep 3
pre_trace_count="$(find "$trace_root" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"
if [[ "$pre_trace_count" -ne 0 ]]; then
  echo "Trace output already exists before attach" >&2
  exit 1
fi

echo "[smoke-attach] Attaching agent to PID $target_pid..."
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
grep -q "ARGS |" <<<"$trace_text"
grep -q "{com.monarchit.target.TargetApp.TargetApp} | INGRESS | ARGS" <<<"$trace_text"
grep -q "RET |" <<<"$trace_text"
grep -q "STACK" <<<"$trace_text"
grep -q "{com.monarchit.target.TargetApp.filteredStackMethod} | INGRESS | STACK" <<<"$trace_text"
grep -q "PROFILE | Execution time" <<<"$trace_text"
grep -q "ADD_MARKER" <<<"$trace_text"
grep -q "CODEPOINT_MARKER" <<<"$trace_text"
grep -q "HEAP" <<<"$trace_text"

heap_count="$(find "$trace_dir" -maxdepth 1 -type f -name "*.hprof" | wc -l | tr -d ' ')"
if [[ "$heap_count" -lt 1 ]]; then
  echo "No heap dump file generated after attach" >&2
  exit 1
fi

metrics_payload="$(curl -fsS "http://127.0.0.1:$metrics_port/metrics")"
grep -q '# HELP monarch_agent_info' <<<"$metrics_payload"
grep -q 'monarch_agent_info{agent="MonarchJavaAgent"} 1.0' <<<"$metrics_payload"

metrics_content_type="$(curl -fsSI "http://127.0.0.1:$metrics_port/metrics" | tr -d '\r' | awk -F': ' 'tolower($1)=="content-type"{print $2}' | tail -n 1)"
if [[ "$metrics_content_type" != text/plain* ]]; then
  echo "Metrics endpoint did not return Prometheus text content type after attach" >&2
  exit 1
fi

openmetrics_payload="$(curl -fsS -H 'Accept: application/openmetrics-text; version=1.0.0' "http://127.0.0.1:$metrics_port/metrics")"
grep -q '# EOF' <<<"$openmetrics_payload"

openmetrics_content_type="$(curl -fsSI -H 'Accept: application/openmetrics-text; version=1.0.0' "http://127.0.0.1:$metrics_port/metrics" | tr -d '\r' | awk -F': ' 'tolower($1)=="content-type"{print $2}' | tail -n 1)"
if [[ "$openmetrics_content_type" != application/openmetrics-text* ]]; then
  echo "OpenMetrics content type not returned after attach" >&2
  exit 1
fi

metrics_json_payload="$(curl -fsS "http://127.0.0.1:$metrics_port/metrics.json")"
grep -q '"agent":"MonarchJavaAgent"' <<<"$metrics_json_payload"

echo "[smoke-attach] PASS"
echo "[smoke-attach] Trace: $trace_file"
echo "[smoke-attach] Run dir: $run_dir"
