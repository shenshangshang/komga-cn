#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: $0 BASELINE" >&2
  exit 2
fi
[ -f "$1" ] && [ -r "$1" ] || { echo "baseline must be a readable regular file: $1" >&2; exit 2; }

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT HUP INT TERM

sed -nE "s/^[[:space:]]*path:[[:space:]]*['\"]([^'\"]+)['\"].*/\1/p" \
  "$root/komga-webui/src/router.ts" | LC_ALL=C sort -u > "$tmp"
[ -s "$tmp" ] || { echo "route extraction produced zero routes" >&2; exit 1; }
baseline_count=$(wc -l < "$1" | tr -d ' ')
current_count=$(wc -l < "$tmp" | tr -d ' ')
[ "$current_count" -eq "$baseline_count" ] || echo "route count changed: baseline=$baseline_count current=$current_count" >&2
diff -u "$1" "$tmp"
