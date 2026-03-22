#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[smoke-all] Running startup attach smoke..."
bash "$script_dir/smoke-javaagent.sh"

echo "[smoke-all] Running runtime attach smoke..."
bash "$script_dir/smoke-attach.sh"

echo "[smoke-all] PASS"
