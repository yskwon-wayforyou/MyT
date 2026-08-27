#!/usr/bin/env bash
# Partner register using tesla.local.properties (NA + EU).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/tesla.local.properties"
DOMAIN="$(grep -E '^tesla.partner.domain=' "$PROPS" | cut -d= -f2- | head -1)"

if [[ -z "$DOMAIN" ]]; then
  echo "Set tesla.partner.domain in $PROPS" >&2
  exit 1
fi

TOKEN="$( "$ROOT/scripts/tesla-partner-token.sh" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])" )"
exec "$ROOT/scripts/tesla-register.sh" "$DOMAIN" "$TOKEN"
