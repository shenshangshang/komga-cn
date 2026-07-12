#!/bin/sh
set -eu

base=${1:-http://127.0.0.1:25610}
attempts=${SMOKE_ATTEMPTS:-60}
case "$attempts" in ''|*[!0-9]*|0) echo "SMOKE_ATTEMPTS must be a positive integer" >&2; exit 2;; esac
i=1
health=''
while [ "$i" -le "$attempts" ]; do
  health=$(curl --connect-timeout 2 --max-time 5 -fsS "$base/actuator/health" 2>&1 || true)
  case "$health" in *'"status":"UP"'*) break;; esac
  [ "$i" -lt "$attempts" ] || { echo "health did not become UP; last response: $health" >&2; exit 1; }
  sleep 2
  i=$((i + 1))
done

html=$(curl --connect-timeout 2 --max-time 10 -fsS "$base/")
printf '%s' "$html" | grep -Fq '<div id="app">' || { echo "root lacks Komga SPA app marker" >&2; exit 1; }
printf '%s' "$html" | grep -Eq '(js/app\.|src="/js/)' || { echo "root lacks Komga SPA asset marker" >&2; exit 1; }
code=$(curl --connect-timeout 2 --max-time 10 -sS -o /dev/null -w '%{http_code}' "$base/api/v1/users/me")
case "$code" in 401|403) ;; *) echo "unexpected unauthenticated status: $code" >&2; exit 1;; esac
echo "smoke passed: health=UP root=SPA auth=$code"
