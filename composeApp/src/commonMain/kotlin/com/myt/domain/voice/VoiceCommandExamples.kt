package com.myt.domain.voice

/**
 * Canonical voice examples used by UI chips, TTS playback, and automated tests.
 * [spokenText] is both the TTS script and the STT-substitute transcript.
 */
data class VoiceCommandExample(
    val id: String,
    val spokenText: String,
    val capability: String,
    val notes: String = "",
)

object VoiceCommandExamples {
    val all: List<VoiceCommandExample> = listOf(
        VoiceCommandExample(
            id = "nav_gangnam",
            spokenText = "강남역으로 안내해줘",
            capability = "내비 목적지",
        ),
        VoiceCommandExample(
            id = "nav_gwanggyo",
            spokenText = "광교중앙역 내비",
            capability = "내비 목적지",
        ),
        VoiceCommandExample(
            id = "call_number",
            spokenText = "전화 01012345678",
            capability = "전화 걸기",
            notes = "번호 포함 필수",
        ),
        VoiceCommandExample(
            id = "sms_open",
            spokenText = "문자 앱 열어줘",
            capability = "문자 보내기",
        ),
        VoiceCommandExample(
            id = "kakao_share",
            spokenText = "카카오톡으로 도착했어 보내줘",
            capability = "카카오톡",
        ),
        VoiceCommandExample(
            id = "history",
            spokenText = "최근 주행 기록 보여줘",
            capability = "히스토리",
        ),
        VoiceCommandExample(
            id = "read_aloud",
            spokenText = "최근 알림 읽어줘",
            capability = "읽어줘",
        ),
        VoiceCommandExample(
            id = "settings",
            spokenText = "설정 열어줘",
            capability = "설정",
        ),
        VoiceCommandExample(
            id = "ytm_lee_seunghwan",
            spokenText = "유튜브 뮤직에서 이승환 2집 음악을 무작위로 플레이해줘",
            capability = "유튜브 뮤직",
        ),
        VoiceCommandExample(
            id = "ytm_artist",
            spokenText = "유튜브 뮤직에서 아이유 노래 틀어줘",
            capability = "유튜브 뮤직",
        ),
    )

    fun byId(id: String): VoiceCommandExample? = all.find { it.id == id }

    val capabilityLabels: List<String> = listOf(
        "전화 걸기",
        "문자 보내기",
        "카카오톡",
        "내비 목적지",
        "히스토리",
        "읽어줘",
        "유튜브 뮤직",
    )

    val listeningHint: String =
        "예) 「강남역으로 안내해줘」 · 「전화 01012345678」 · 「유튜브 뮤직에서 이승환 2집 플레이」 · 「최근 알림 읽어줘」"
}
