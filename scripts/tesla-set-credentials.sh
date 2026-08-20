#!/usr/bin/env bash
# Interactive helper: paste values from Tesla Developer dashboard into tesla.local.properties
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/tesla.local.properties"

echo "Tesla Developer dashboard:"
echo "https://developer.tesla.com/ko_KR/dashboard/app-details/c0ece73a-3df9-46ba-b994-373dd6cf4515"
echo

read -r -p "Client ID: " CLIENT_ID
read -r -s -p "Client Secret: " CLIENT_SECRET; echo
read -r -p "Partner domain (allowed origin, e.g. example.com): " DOMAIN
read -r -p "Redirect URI [myt://auth/callback]: " REDIRECT
REDIRECT="${REDIRECT:-myt://auth/callback}"
read -r -p "Model 3 VIN (optional): " VIN

cat > "$OUT" <<EOF
tesla.app.id=c0ece73a-3df9-46ba-b994-373dd6cf4515
tesla.client.id=${CLIENT_ID}
tesla.client.secret=${CLIENT_SECRET}
tesla.oauth.redirect.uri=${REDIRECT}
tesla.partner.domain=${DOMAIN}
tesla.fleet.api.base=https://fleet-api.prd.na.vn.cloud.tesla.com
tesla.auth.url=https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3
tesla.private.key.path=secrets/tesla/private-key.pem
tesla.vehicle.vin=${VIN}
EOF

chmod 600 "$OUT"
echo "Wrote $OUT (mode 600)"
