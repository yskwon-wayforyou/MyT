#!/usr/bin/env bash
# Register MyT partner domain with Tesla Fleet API (NA + EU).
# Usage: ./scripts/tesla-register.sh <domain> <partner_access_token>
set -euo pipefail

DOMAIN="${1:?Usage: $0 <domain> <partner_token>}"
PARTNER_TOKEN="${2:?Usage: $0 <domain> <partner_token>}"

register_region() {
  local base_url="$1"
  local region="$2"
  echo "==> Registering $DOMAIN in $region ($base_url)"
  http_code=$(curl -s -o /tmp/tesla-register-response.json -w "%{http_code}" \
    -X POST "$base_url/api/1/partner_accounts" \
    -H "Authorization: Bearer $PARTNER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"domain\": \"$DOMAIN\"}")
  echo "HTTP $http_code"
  cat /tmp/tesla-register-response.json
  echo
}

verify_key() {
  local base_url="$1"
  local region="$2"
  echo "==> Verifying public key ($region)"
  curl -s "$base_url/api/1/partner_accounts/public_key?domain=$DOMAIN" \
    -H "Authorization: Bearer $PARTNER_TOKEN"
  echo
}

register_region "https://fleet-api.prd.na.vn.cloud.tesla.com" "NA"
verify_key "https://fleet-api.prd.na.vn.cloud.tesla.com" "NA"

register_region "https://fleet-api.prd.eu.vn.cloud.tesla.com" "EU"
verify_key "https://fleet-api.prd.eu.vn.cloud.tesla.com" "EU"

echo "Done. Next: open https://tesla.com/_ak/$DOMAIN on your phone for virtual key pairing."
