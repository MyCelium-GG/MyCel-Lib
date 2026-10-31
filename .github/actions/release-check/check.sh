#!/usr/bin/env bash
# Fails when a release tag and what is being released disagree: the version in
# gradle.properties, the version the mod reports in its code, and the changelog
# section that becomes the CurseForge notes. Run from a repo root:
#
#   check.sh 1.2.0
set -u
TAG="${1:?usage: check.sh VERSION}"
fail=0
error() { echo "::error::$1"; fail=1; }

version=$(grep -m1 '^version=' gradle.properties | cut -d= -f2- | xargs)
[ "$version" = "$TAG" ] || error "Tag $TAG, but gradle.properties says version=$version."

code=$(grep -rl 'ModMetadata.builder(' common/src/main/java 2>/dev/null)
if [ -z "$code" ]; then
  error "No ModMetadata.builder( found under common/src/main/java."
elif ! echo "$code" | tr '\n' '\0' | xargs -0 grep -qF "\"$TAG\""; then
  error "Tag $TAG, but the code does not report that version. Update it next to ModMetadata.builder."
fi

grep -qF "## [$TAG]" CHANGELOG.md || error "CHANGELOG.md has no '## [$TAG]' section."

[ "$fail" = 0 ] && echo "Version $TAG matches gradle.properties, the code and CHANGELOG.md."
exit "$fail"
