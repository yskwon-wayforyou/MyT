package com.myt.platform

/**
 * M24 — 로컬 기반 크래시 리포팅( Crashlytics 대체/보완 ).
 *
 * Firebase Crashlytics 완전 통합 이전 단계에서,
 * “마지막 크래시”를 파일로 저장해 디버깅/제보 시 활용합니다.
 */
expect class CrashReporterPlatform(context: Any) {
    fun install()
    fun lastCrashReport(): String?
    fun clearLastCrashReport()
}

