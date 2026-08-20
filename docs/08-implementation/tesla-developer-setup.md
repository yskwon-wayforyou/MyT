# Tesla Fleet API — Developer 등록 가이드 (Phase 1 개인용)

> MyT Phase 1: 본인 Tesla Model 3 1대만 연동  
> Tesla 계정: **7yskwon@gmail.com** (로그인·MFA는 오빠가 직접 진행)

커서는 Tesla Developer Portal에 로그인할 수 없습니다. 아래 순서대로 진행하시면 되고, 각 단계 완료 후 체크해 주세요.

## MyT 앱 (등록됨)

| 항목 | 값 |
|---|---|
| App UUID | `c0ece73a-3df9-46ba-b994-373dd6cf4515` |
| Dashboard | [앱 상세](https://developer.tesla.com/ko_KR/dashboard/app-details/c0ece73a-3df9-46ba-b994-373dd6cf4515) |
| OAuth Redirect (MyT) | `myt://auth/callback` |
| 로컬 설정 | `tesla.local.properties` (gitignored) |

Client ID / Secret 은 대시보드에서 복사 후:

```bash
./scripts/tesla-set-credentials.sh
```

또는 `tesla.local.properties` 에 직접 입력하세요.

---

## 사전 준비 (완료 여부)

| 항목 | 상태 | 비고 |
|---|---|---|
| JDK 17+ | ✅ | `/Users/wayforyou/.jdks/jdk-17.0.20+8` |
| EC 키쌍 | ✅ | `secrets/tesla/private-key.pem`, `public-key.pem` |
| Android SDK | ✅ | `local.properties` → `~/Library/Android/sdk` |
| **도메인** | ⬜ | 공개키 호스팅용 (아래 3절 참고) |

---

## Step 1 — Tesla 계정 확인

1. [tesla.com](https://www.tesla.com) 에 **7yskwon@gmail.com** 으로 로그인
2. **이메일 인증** 완료
3. **2단계 인증(MFA)** 활성화 (SMS 또는 Authenticator)

> MFA 없으면 Developer Portal 앱 생성이 거절될 수 있습니다.

---

## Step 2 — Developer Portal 앱 생성

1. [developer.tesla.com](https://developer.tesla.com) 접속 → **Sign In** (위 Tesla 계정)
2. **Create Application** (또는 Request app access)
3. 아래 값으로 입력:

| 필드 | 권장 값 |
|---|---|
| Application name | `MyT-Personal` (고유해야 함, 중복 시 거절) |
| Description | Personal Tesla Model 3 dashboard companion for private use |
| Allowed origins / Redirect URI | `myt://auth/callback` (앱 딥링크) + 필요 시 `http://localhost:8080/callback` (개발용) |
| Domain | 공개키를 올릴 **루트 도메인** (예: `example.com`) |

4. **Scopes** — MyT Phase 1에 필요한 것만 선택:

| Scope | 용도 |
|---|---|
| `openid` | Sign in with Tesla |
| `offline_access` | Refresh token (재로그인 없이 유지) |
| `vehicle_device_data` | 속도·SOC·기어 등 계기판 |
| `vehicle_location` | GPS·내비·RouteLine (과속카메라·지도) |
| `vehicle_cmds` | navigation_request (음성 내비) |

5. 생성 후 **Client ID**, **Client Secret** 을 메모 → `tesla.local.properties` 에 저장 (아래 Step 6)

공식 문서: [What is Fleet API?](https://developer.tesla.com/docs/fleet-api/getting-started/what-is-fleet-api)

---

## Step 3 — 공개키 도메인 호스팅

Tesla는 아래 URL에서 **EC P-256 공개키**를 확인합니다:

```
https://<YOUR_DOMAIN>/.well-known/appspecific/com.tesla.3p.public-key.pem
```

### 이미 생성된 키 (프로젝트)

```bash
# 공개키 확인
cat /Users/wayforyou/Projects/MyT/secrets/tesla/public-key.pem
```

### Phase 1 도메인 옵션

| 방법 | 난이도 | 설명 |
|---|---|---|
| **본인 도메인** | 중 | Cloudflare Pages / GitHub Pages / S3+CloudFront |
| **임시 터널** | 쉬움(개발만) | ngrok 등 — URL이 바뀌면 재등록 필요 |
| **Phase 2 전용 도메인** | — | 상용 전 `myt.app` 등 확정 |

호스팅 예 (GitHub Pages):

1. `https://<user>.github.io` 또는 커스텀 도메인
2. 경로: `.well-known/appspecific/com.tesla.3p.public-key.pem`
3. `public-key.pem` 내용 그대로 업로드 (PEM 텍스트, `Content-Type: application/x-pem-file` 또는 `text/plain`)

검증:

```bash
curl -s "https://<YOUR_DOMAIN>/.well-known/appspecific/com.tesla.3p.public-key.pem"
# -----BEGIN PUBLIC KEY----- 로 시작해야 함
```

---

## Step 4 — Partner Token + Register (리전별)

Developer Portal에서 **Partner authentication token** 을 발급한 뒤, **운영 리전마다** register API를 호출합니다.

한국 차량은 보통 **North America** 또는 **Europe** Fleet API 리전에 매핑됩니다. 둘 다 등록하는 것이 안전합니다.

### 4.1 Partner Token 발급

Developer Portal → Application → **Generate token** (Partner token, `client_credentials`)

또는:

```bash
curl -s -X POST "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=<CLIENT_ID>" \
  -d "client_secret=<CLIENT_SECRET>" \
  -d "audience=https://fleet-api.prd.na.vn.cloud.tesla.com" \
  -d "scope=openid vehicle_device_data vehicle_cmds vehicle_location vehicle_charging_cmds"
```

응답의 `access_token` 을 `PARTNER_TOKEN` 으로 사용합니다.

### 4.2 Register — North America

```bash
export PARTNER_TOKEN="<partner_access_token>"
export DOMAIN="<YOUR_DOMAIN>"

curl -s -X POST "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/partner_accounts" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"domain\": \"$DOMAIN\"}"
```

### 4.3 Register — Europe (권장: 추가 호출)

```bash
curl -s -X POST "https://fleet-api.prd.eu.vn.cloud.tesla.com/api/1/partner_accounts" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"domain\": \"$DOMAIN\"}"
```

> `412 Unregistered account` → register 미완료  
> `403` → 공개키 URL 또는 domain 불일치  
> `421 Incorrect region` → 다른 리전 register 필요

### 4.4 등록 확인

```bash
curl -s "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/partner_accounts/public_key?domain=$DOMAIN" \
  -H "Authorization: Bearer $PARTNER_TOKEN"
```

공식: [Partner Endpoints — register](https://developer.tesla.com/docs/fleet-api/endpoints/partner-endpoints#register)

프로젝트 스크립트:

```bash
./scripts/tesla-register.sh <DOMAIN> <PARTNER_TOKEN>
```

---

## Step 5 — Virtual Key 페어링 (차량 명령용)

`vehicle_cmds` (내비 목적지 설정 등)를 쓰려면 차량에 **가상 키**를 페어링해야 합니다.

1. 휴대폰 브라우저에서:

```
https://tesla.com/_ak/<YOUR_DOMAIN>
```

2. Tesla 앱으로 열기 → 본인 Model 3 선택 → **Allow** / 키 추가
3. 차량 화면에 앱 이름(도메인)이 표시되는지 확인

공식: [Virtual Keys](https://developer.tesla.com/docs/fleet-api/virtual-keys/overview)

---

## Step 6 — MyT 프로젝트 설정

1. 예시 파일 복사:

```bash
cp tesla.local.properties.example tesla.local.properties
```

2. Developer Portal에서 받은 값 입력:

```properties
tesla.client.id=<CLIENT_ID>
tesla.client.secret=<CLIENT_SECRET>
tesla.oauth.redirect.uri=myt://auth/callback
tesla.partner.domain=<YOUR_DOMAIN>
tesla.fleet.api.base=https://fleet-api.prd.na.vn.cloud.tesla.com
tesla.auth.url=https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3
tesla.private.key.path=secrets/tesla/private-key.pem
```

3. **절대 커밋하지 않기** — `tesla.local.properties`, `secrets/` 는 `.gitignore` 처리됨

---

## Step 7 — Third-party OAuth (앱 로그인)

사용자(본인) 토큰은 **Authorization Code + PKCE** 로 발급합니다.

| 항목 | 값 |
|---|---|
| Authorize | `https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/authorize` |
| Token | `https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token` |
| Metadata | `https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/thirdparty/.well-known/openid-configuration` |

MyT 온보딩 화면의 「Tesla로 로그인」이 이 flow를 호출합니다 (M3 구현 예정).

공식: [Authentication Overview](https://developer.tesla.com/docs/fleet-api/authentication/overview)

---

## Step 8 — 차량 연결 확인

등록·OAuth 완료 후:

```bash
# Third-party access token 필요
curl -s "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/vehicles" \
  -H "Authorization: Bearer <USER_ACCESS_TOKEN>"
```

VIN을 확인한 뒤 `VehicleConfig` / 앱 설정에 입력합니다.

---

## 체크리스트 (오빠용)

- [ ] Tesla 계정 MFA 활성화
- [ ] developer.tesla.com 앱 생성 (`MyT-Personal`)
- [ ] Scopes 5개 선택
- [ ] Client ID / Secret 저장 → `tesla.local.properties`
- [ ] 도메인에 `public-key.pem` 호스팅
- [ ] NA (+ EU) register API 성공
- [ ] `tesla.com/_ak/<domain>` 가상 키 페어링
- [ ] OAuth 로그인 → vehicle list에 Model 3 표시
- [ ] MyT 앱에서 VIN 설정

---

## 문제 해결

| 증상 | 조치 |
|---|---|
| 앱 생성 자동 거절 | 앱 이름 변경 (`MyT-Personal-<이니셜>`) |
| register 403 | 공개키 URL·도메인·allowed_origins 일치 확인 |
| 421 Incorrect region | NA/EU/CN register 각각 시도 |
| vehicle_cmds 403 | Virtual Key 미페어링 → Step 5 |
| location 데이터 없음 | `vehicle_location` scope + 차량 FW 2023.38+ |
| 차량 sleep | `wake_up` API 후 `vehicle_data` 폴링 |

---

## 다음 (커서가 코드로 진행)

1. M3 OAuth PKCE (`AuthUseCase` + 딥링크)
2. `KtorFleetRepository` 실 API 연동
3. `progress-tracker` P1-M3-T03 완료 처리

도메인을 정하시면 공개키 업로드·register curl까지 구체적으로 맞춰 드리겠습니다.
