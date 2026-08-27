#!/usr/bin/env python3
"""
Download Korea national speed-camera CSV from data.go.kr and convert for MyT OTA.

Requires a free data.go.kr API key:
  export DATA_GO_KR_API_KEY=...

Usage:
  python3 scripts/fetch_national_poi.py --out composeApp/src/commonMain/composeResources/files/poi/speed_cameras_national.csv

Then host the CSV (GitHub raw / S3 / CDN) and set:
  tesla.poi.ota.csv.url=<https://.../speed_cameras_national.csv>
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
import urllib.parse
import urllib.request

# Official national unmanned traffic camera standard dataset endpoint (OpenAPI).
DEFAULT_ENDPOINT = (
    "https://api.odcloud.kr/api/15028200/v1/"
    "uddi:99b75728-5f66-4e3d-a0f2-8b3b0d4f0a0a"
)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True, help="Output CSV path")
    ap.add_argument("--api-key", default=os.environ.get("DATA_GO_KR_API_KEY", ""))
    ap.add_argument("--endpoint", default=os.environ.get("DATA_GO_KR_POI_URL", DEFAULT_ENDPOINT))
    ap.add_argument("--page-size", type=int, default=1000)
    args = ap.parse_args()

    if not args.api_key:
        print(
            "DATA_GO_KR_API_KEY missing.\n"
            "1) Sign up at https://www.data.go.kr\n"
            "2) Apply for 전국무인교통단속카메라표준데이터 API\n"
            "3) export DATA_GO_KR_API_KEY=...\n"
            "Falling back: copy bundled sample CSV instead.",
            file=sys.stderr,
        )
        sample = os.path.join(
            os.path.dirname(__file__),
            "..",
            "composeApp/src/commonMain/composeResources/files/poi/speed_cameras_bundle.csv",
        )
        sample = os.path.abspath(sample)
        if not os.path.exists(sample):
            raise SystemExit("Sample CSV not found")
        os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
        with open(sample, "r", encoding="utf-8") as src, open(args.out, "w", encoding="utf-8") as dst:
            dst.write(src.read())
        print(f"Wrote sample bundle to {args.out}")
        return

    # Paginated OpenAPI fetch (JSON → CSV). Endpoint IDs change; override with --endpoint.
    rows: list[dict] = []
    page = 1
    while True:
        qs = urllib.parse.urlencode(
            {
                "serviceKey": args.api_key,
                "page": page,
                "perPage": args.page_size,
                "returnType": "JSON",
            }
        )
        url = f"{args.endpoint}?{qs}"
        with urllib.request.urlopen(url, timeout=60) as resp:
            import json

            payload = json.loads(resp.read().decode("utf-8"))
        data = payload.get("data") or payload.get("response", {}).get("body", {}).get("items") or []
        if isinstance(data, dict):
            data = data.get("item") or []
        if not data:
            break
        rows.extend(data)
        if len(data) < args.page_size:
            break
        page += 1
        if page > 200:
            break

    if not rows:
        raise SystemExit("No rows returned — check API key / endpoint")

    fieldnames = [
        "무인교통단속카메라관리번호",
        "위도",
        "경도",
        "도로노선방향",
        "제한속도",
        "도로명",
        "단속구분",
        "구간길이",
    ]
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        for r in rows:
            w.writerow(
                {
                    "무인교통단속카메라관리번호": r.get("무인교통단속카메라관리번호") or r.get("id") or "",
                    "위도": r.get("위도") or r.get("latitude") or "",
                    "경도": r.get("경도") or r.get("longitude") or "",
                    "도로노선방향": r.get("도로노선방향") or r.get("direction") or "",
                    "제한속도": r.get("제한속도") or r.get("speed") or "",
                    "도로명": r.get("도로노선명") or r.get("도로명") or "",
                    "단속구분": r.get("단속구분") or "",
                    "구간길이": r.get("과속단속구간길이") or r.get("구간길이") or "",
                }
            )
    print(f"Wrote {len(rows)} cameras to {args.out}")


if __name__ == "__main__":
    main()
