#!/usr/bin/env bash
# Prepares a mod release, from the folder that holds all MyCelium repos:
#
#   MyCelium/scripts/bump_mod.sh "Better Pets" 1.3.0
#
# Sets the version in gradle.properties and next to ModMetadata.builder, and
# turns the changelog's [Unreleased] section into this version with today's
# date. Review it, commit, tag and push; nothing is committed here.
set -euo pipefail
[ $# -eq 2 ] || { echo "usage: $0 MOD_DIR VERSION" >&2; exit 2; }
MOD="$1"
NEW="$2"
OLD=$(grep -m1 '^version=' "$MOD/gradle.properties" | cut -d= -f2-)
grep -q '^## \[Unreleased\]' "$MOD/CHANGELOG.md" || { echo "$MOD: CHANGELOG.md has no [Unreleased] section" >&2; exit 1; }
sed -i "s/^version=.*/version=$NEW/" "$MOD/gradle.properties"
grep -rl 'ModMetadata.builder(' "$MOD/common/src/main/java" | while IFS= read -r file; do
  sed -i "/ModMetadata.builder(/s/\"$OLD\"/\"$NEW\"/" "$file"
done
sed -i "s/^## \[Unreleased\]/## [$NEW] - $(date +%F)/" "$MOD/CHANGELOG.md"
CHECK="$(cd "$(dirname "$0")/.." && pwd)/.github/actions/release-check/check.sh"
cd "$MOD" && "$CHECK" "$NEW"
