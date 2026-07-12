#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: $0 BASELINE" >&2
  exit 2
fi

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT HUP INT TERM

sed -nE "s/^[[:space:]]*path:[[:space:]]*['\"]([^'\"]+)['\"].*/\1/p" \
  "$root/komga-webui/src/router.ts" | LC_ALL=C sort -u > "$tmp"
diff -u "$1" "$tmp"
