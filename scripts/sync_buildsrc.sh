#!/usr/bin/env bash
# Writes the shared mod buildSrc (.github/actions/buildsrc-check/buildSrc) into
# every mod checkout given, e.g. from the folder that holds all MyCelium repos:
#
#   MyCelium/scripts/sync_buildsrc.sh "Better Pets" "Brewing+" ...
#
# Each mod's CI fails until its buildSrc matches the shared copy again.
set -euo pipefail
SRC="$(cd "$(dirname "$0")/.." && pwd)/.github/actions/buildsrc-check/buildSrc"
[ $# -gt 0 ] || { echo "usage: $0 MOD_DIR..." >&2; exit 2; }
for mod in "$@"; do
  [ -f "$mod/gradle.properties" ] || { echo "skip $mod: not a mod checkout" >&2; continue; }
  rm -rf "$mod/buildSrc/src" "$mod/buildSrc/build.gradle"
  mkdir -p "$mod/buildSrc"
  cp -r "$SRC/." "$mod/buildSrc/"
  echo "synced $mod"
done
