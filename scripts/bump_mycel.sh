#!/usr/bin/env bash
# Moves every mod given to a new Mycel release, from the folder that holds all
# MyCelium repos (mods always require the latest Mycel):
#
#   MyCelium/scripts/bump_mycel.sh 1.6.0 "Better Pets" "Brewing+" ...
#
# Edits gradle.properties only; nothing is committed here.
set -euo pipefail
[ $# -ge 2 ] || { echo "usage: $0 VERSION MOD_DIR..." >&2; exit 2; }
NEW="$1"
shift
for mod in "$@"; do
  file="$mod/gradle.properties"
  [ -f "$file" ] || { echo "skip $mod: not a mod checkout" >&2; continue; }
  sed -i "s/^mycel_version=.*/mycel_version=$NEW/; s/^\(mycel_coordinates=.*:\)[^:]*$/\1$NEW/" "$file"
  echo "$mod: $(grep -m1 '^mycel_version=' "$file")"
done
