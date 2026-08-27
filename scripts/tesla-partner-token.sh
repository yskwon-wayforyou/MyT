#!/usr/bin/env bash
# Read tesla.local.properties and fetch a partner access token.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/tesla.local.properties"

if [[ ! -f "$PROPS" ]]; then
  echo "Missing $PROPS — copy from tesla.local.properties.example" >&2
  exit 1
fi

get_prop() {
  grep -E "^$1=" "$PROPS" | cut -d= -f2- | head -1
}

CLIENT_ID="$(get_prop tesla.client.id)"
CLIENT_SECRET="$(get_prop tesla.client.secret)"
FLEET_BASE="$(get_prop tesla.fleet.api.base)"
FLEET_BASE="${FLEET_BASE:-https://fleet-api.prd.na.vn.cloud.tesla.com}"

if [[ -z "$CLIENT_ID" || -z "$CLIENT_SECRET" ]]; then
  echo "tesla.client.id / tesla.client.secret required in $PROPS" >&2
  exit 1
fi

curl -s -X POST "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode "client_secret=$CLIENT_SECRET" \
  --data-urlencode "audience=$FLEET_BASE" \
  --data-urlencode "scope=openid vehicle_device_data vehicle_location vehicle_cmds offline_access"
