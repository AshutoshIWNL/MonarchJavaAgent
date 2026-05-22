# Releasing MonarchJavaAgent

This repository uses a tag-driven GitHub Actions workflow to publish releases.

Workflow file: `.github/workflows/release.yml`

## Supported tag formats

- `v<version>` (example: `v1.3`)
- `<version>.RELEASE` (example: `1.3.RELEASE`)

The release workflow validates that the tag version matches `pom.xml` version exactly.

## Release checklist

1. Ensure `pom.xml` has the intended release version.
2. Run local verification:
   - `mvn clean verify`
3. Push `master`:
   - `git push origin master`
4. Create and push release tag (pick one format):
   - `git tag v1.3`
   - `git push origin v1.3`
   - or
   - `git tag 1.3.RELEASE`
   - `git push origin 1.3.RELEASE`
5. Watch GitHub Actions `Release` workflow.
6. Verify GitHub Release assets include:
   - `MonarchJavaAgent-<version>-all.jar`
   - `MonarchJavaAgent-<version>-all.jar.sha256`

## Common failure cases

- Tag/version mismatch:
  - Example: tag `v1.3` but `pom.xml` is `1.2`
  - Fix: update `pom.xml` or retag with the correct version.
- Missing `-all.jar` artifact:
  - Build/package configuration changed unexpectedly.
  - Fix: run `mvn clean package` locally and confirm `target/*-all.jar` exists.

## Post-release

1. Confirm release notes and downloadable assets are correct.
2. If needed, bump `pom.xml` to the next development version and push.
