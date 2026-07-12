#!/bin/sh
set -eu

base=${1:-http://127.0.0.1:25610}
attempts=${SMOKE_ATTEMPTS:-60}
i=1
while [ "$i" -le "$attempts" ]; do
  health=$(curl -fsS "$base/actuator/health" 2>/dev/null || true)
  case "$health" in *'"status":"UP"'*) break;; esac
  [ "$i" -lt "$attempts" ] || { echo "health did not become UP" >&2; exit 1; }
  sleep 2
  i=$((i + 1))
done

html=$(curl -fsS "$base/")
printf '%s' "$html" | grep -Eqi '<!doctype html|<html' || { echo "root is not SPA HTML" >&2; exit 1; }
code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/api/v1/users/me")
case "$code" in 401|403) ;; *) echo "unexpected unauthenticated status: $code" >&2; exit 1;; esac
echo "smoke passed: health=UP root=SPA auth=$code"
