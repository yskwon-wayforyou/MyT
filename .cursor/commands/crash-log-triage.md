---
description: 단말에서 MyT 크래시/런타임 로그를 수집하고 GitHub 이슈를 등록한 뒤, 로그 근거로 코드를 수정한다
---

# crash-log-triage

앱 강제 종료·Exception 이후 **자동 생성된 로그/펜딩 이슈**를 단말이 연결되어 있으면 수집하고, GitHub Issue를 만든 다음, 원인을 고쳐 재설치까지 진행한다.

## When to use

- 사용자가 “앱이 강제 종료된다 / 크래시 / 비정상 종료”를 보고할 때
- `/device-apk-debug-loop` 후에도 원인이 로그 파일에 남아 있을 때
- 사용자가 크래시 분석·수정을 에이전트에게 위임했을 때 (**기본으로 이 커맨드 우선**)

## Data locations (on device, package `com.myt`)

| Path | Contents |
|------|----------|
| `files/debug_logs/myt-runtime.log` | WARN/ERROR 실시간 로그 (회전) |
| `files/crash_reports/myt-last-crash.txt` | 마지막 uncaught exception |
| `files/pending_github_issues/*.json` + `*.md` | GitHub 이슈 펜딩 큐 |

App cold start (`CrashIssueSyncUseCase`)가 크래시 파일을 펜딩 이슈로 바꾸고, `github.issues.token`이 있으면 API로 업로드한다.  
토큰이 없으면 펜딩 파일이 남고, 이 스크립트가 `gh`로 이슈를 만든다.

## Agent procedure

### 1) Pull + file issues

```bash
export DEVELOPER_DIR=/Library/Developer/CommandLineTools
chmod +x scripts/crash_log_triage.sh
./scripts/crash_log_triage.sh all
```

읽기: `build/device-debug/crash-logs/<stamp>/SUMMARY.md`

### 2) Diagnose (evidence only)

- `SUMMARY.md`의 crash stack + runtime log tail
- pending `*.md` body
- 필요 시 `./scripts/device_apk_debug_loop.sh collect`

추측으로 고치지 않는다. 스택/로그에 나온 클래스·메시지를 근거로 최소 수정한다.

### 3) Fix → rebuild → reinstall

```bash
./scripts/device_apk_debug_loop.sh all
```

### 4) Report (Korean, 오빠 호칭)

- 수집된 로그 요약 (스택 한 줄)
- 생성/연결된 GitHub issue URL
- 수정 파일과 재설치 결과
- 잠금 화면 등으로 UI 확인이 안 되면 그 사실만 짧게

## Config (optional in-app upload)

`tesla.local.properties`:

```
github.issues.enabled=true
github.issues.repo=yskwon-wayforyou/MyT
github.issues.token=ghp_...   # issues:write only; never commit / never chat-print
```

Without token, app still writes pending files; agent + `gh` completes filing.

## Do not

- GitHub token / Tesla secret을 채팅·커밋·이슈 본문에 넣지 않는다 (LogRedactor + 수동 확인)
- 단말 잠금을 우회하지 않는다
- 로그에 없는 대규모 리팩터를 하지 않는다
