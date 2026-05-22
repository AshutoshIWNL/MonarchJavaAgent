# Changelog

## 1.3 (2026-05-22)

### Features
- Added `MLOG(...)` support inside `ADD` rules to route custom diagnostic output to Monarch trace logs.
- Added preflight validation mode (`preflight=true`) for config/rule validation without starting instrumentation/observer components.
- Added structured rule validation diagnostics (accepted/rejected/skipped summary with per-rule reasons/suggestions).

### Testing
- Expanded runtime attach smoke coverage to validate:
  - preflight attach followed by full re-attach
  - standard `ADD` behavior via target process stdout
  - `MLOG(...)` output via Monarch trace file

### Build and Release
- Stabilized shaded artifact output naming to `*-all.jar`.
- Added release workflow guard to enforce tag version and `pom.xml` version match.
- Added release runbook (`RELEASING.md`).
