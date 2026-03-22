#!/usr/bin/env bash
set -euo pipefail

escape_yaml_path() {
  echo "$1" | sed 's/\\/\\\\/g'
}

write_config() {
  local config_file="$1"
  local trace_root="$2"
  local enable_add_rule="$3"
  local escaped_trace_root
  escaped_trace_root="$(escape_yaml_path "$trace_root")"

  {
    cat <<EOF
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$escaped_trace_root"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
EOF
    if [[ "$enable_add_rule" == "true" ]]; then
      echo '  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("RELOAD_MARKER_ON");]'
    fi
    cat <<EOF
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
  } > "$config_file"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

echo "[smoke-config-reload] Building agent..."
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
run_root="/tmp/mja-smoke/config-reload"
timestamp="$(date +%Y%m%d_%H%M%S)"
run_dir="$run_root/$timestamp"
target_classes="$run_dir/target-classes"
trace_root="$run_dir/trace"
agent_log_dir="$run_dir/agent-logs"
config_file="$run_dir/config.yaml"
agent_jar_local="$run_dir/agent.jar"

mkdir -p "$target_classes" "$run_dir" "$trace_root" "$agent_log_dir"
cp "$agent_jar" "$agent_jar_local"

echo "[smoke-config-reload] Compiling target app..."
javac -d "$target_classes" "$target_src"

write_config "$config_file" "$trace_root" "true"

agent_args="configFile=$config_file,agentLogFileDir=$agent_log_dir,agentLogLevel=INFO,agentJarPath=$agent_jar_local"
target_stdout="$run_dir/target.out.log"
target_stderr="$run_dir/target.err.log"

echo "[smoke-config-reload] Starting target app with initial ADD rule..."
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

before_text="$(cat "$trace_file")"
if ! grep -q "RELOAD_MARKER_ON" <<<"$before_text"; then
  echo "Expected RELOAD_MARKER_ON before config change" >&2
  exit 1
fi
before_len="$(wc -c < "$trace_file" | tr -d ' ')"

echo "[smoke-config-reload] Updating config to remove ADD rule..."
write_config "$config_file" "$trace_root" "false"

found_reload_event="false"
found_ret="false"
post_reload_text=""
for _ in $(seq 1 15); do
  sleep 1
  now_len="$(wc -c < "$trace_file" | tr -d ' ')"
  if [[ "$now_len" -gt "$before_len" ]]; then
    tail_text="$(tail -c +$((before_len + 1)) "$trace_file" 2>/dev/null || true)"
    if grep -q "Configuration file has been modified" <<<"$tail_text"; then
      found_reload_event="true"
      post_reload_text="$(awk 'seen{print} /Configuration file has been modified/{seen=1}' <<<"$tail_text")"
      if grep -q "RET |" <<<"$post_reload_text"; then
        found_ret="true"
        break
      fi
    fi
  fi
done

if [[ "$found_reload_event" != "true" ]]; then
  echo "Expected config reload event in trace after file update" >&2
  exit 1
fi

if [[ "$found_ret" != "true" ]]; then
  echo "Expected ongoing RET activity after config reload" >&2
  exit 1
fi

if grep -q "RELOAD_MARKER_ON" <<<"$post_reload_text"; then
  echo "RELOAD_MARKER_ON still present after removing ADD rule" >&2
  exit 1
fi

echo "[smoke-config-reload] PASS"
echo "[smoke-config-reload] Trace: $trace_file"
echo "[smoke-config-reload] Run dir: $run_dir"
