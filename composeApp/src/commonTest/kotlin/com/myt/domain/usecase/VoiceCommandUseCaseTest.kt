package com.myt.domain.usecase

import com.myt.debug.DebugLogger
import com.myt.domain.history.ChargeHistoryItem
import com.myt.domain.history.FleetApiHistoryItem
import com.myt.domain.history.HistoryFilterState
import com.myt.domain.history.TripHistoryItem
import com.myt.domain.model.GaugeState
import com.myt.domain.quota.FleetCallCategory
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.voice.VoiceCommandExamples
import com.myt.platform.DeviceCommunications
import com.myt.platform.SpeechRecognizer
import com.myt.platform.TextToSpeech
import com.myt.test.TestSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VoiceCommandUseCaseTest {
    @Test
    fun examples_injectWithoutTts_allSucceed() = runBlocking {
        val musicQueries = mutableListOf<String>()
        val dialed = mutableListOf<String>()
        val navDest = mutableListOf<String>()
        val uc = useCase(
            musicQueries = musicQueries,
            dialed = dialed,
            navDest = navDest,
            ttsOk = true,
            navOk = true,
        )
        val results = uc.runAllExamplesAsTtsInject(speakFirst = false)
        results.forEach { (ex, result) ->
            assertTrue(
                result !is VoiceCommandResult.Failed,
                "example ${ex.id} failed: ${(result as? VoiceCommandResult.Failed)?.message}",
            )
        }
        assertTrue(musicQueries.any { it.contains("이승환") && it.contains("2집") })
        assertTrue(musicQueries.any { it.contains("아이유") })
        assertTrue(dialed.any { it.contains("01012345678") })
        assertTrue(navDest.any { it.contains("강남") || it.contains("광교") })
    }

    @Test
    fun playExample_ttsFailure_returnsFailed() = runBlocking {
        val uc = useCase(ttsOk = false, navOk = true)
        val example = VoiceCommandExamples.byId("history")!!
        val result = uc.playExampleAndExecute(example, speakFirst = true)
        assertIs<VoiceCommandResult.Failed>(result)
        assertTrue(result.message.contains("TTS"))
    }

    @Test
    fun youtubeMusic_opensSearchQuery() = runBlocking {
        val musicQueries = mutableListOf<String>()
        val uc = useCase(musicQueries = musicQueries, navOk = true)
        val result = uc.execute("유튜브 뮤직에서 이승환 2집 음악을 무작위로 플레이해줘")
        assertEquals(VoiceCommandResult.Sent, result)
        assertEquals(1, musicQueries.size)
        assertTrue(musicQueries[0].contains("이승환"))
        assertTrue(musicQueries[0].contains("2집"))
    }

    @Test
    fun callWithoutNumber_failsHelpfully() = runBlocking {
        val uc = useCase(navOk = true)
        val result = uc.execute("엄마에게 전화")
        assertIs<VoiceCommandResult.Failed>(result)
        assertTrue(result.message.contains("전화번호"))
    }

    private fun useCase(
        musicQueries: MutableList<String> = mutableListOf(),
        dialed: MutableList<String> = mutableListOf(),
        navDest: MutableList<String> = mutableListOf(),
        ttsOk: Boolean = true,
        navOk: Boolean = true,
    ): VoiceCommandUseCase {
        val speech = object : SpeechRecognizer {
            override suspend fun recognizeSpeech(locale: String): Result<String> =
                Result.failure(IllegalStateException("mic unused in test"))
        }
        val tts = object : TextToSpeech {
            override suspend fun speak(text: String, locale: String): Result<Unit> =
                if (ttsOk) Result.success(Unit) else Result.failure(IllegalStateException("tts down"))

            override suspend fun speakAndWait(text: String, locale: String): Result<Unit> = speak(text, locale)
            override fun stop() = Unit
        }
        val comms = object : DeviceCommunications {
            override fun dialPhone(number: String): Result<Unit> {
                dialed += number
                return Result.success(Unit)
            }

            override fun sendSms(number: String, message: String): Result<Unit> = Result.success(Unit)
            override fun shareKakaoTalk(message: String): Result<Unit> = Result.success(Unit)
            override fun openMessagingApp(): Result<Unit> = Result.success(Unit)
            override fun openYouTubeMusicSearch(query: String): Result<Unit> {
                musicQueries += query
                return Result.success(Unit)
            }
        }
        val fleet = object : FleetRepository {
            override fun observeVehicleState(vin: String): Flow<GaugeState> = emptyFlow()
            override suspend fun fetchVehicleState(vin: String): Result<GaugeState> =
                Result.failure(UnsupportedOperationException())
            override suspend fun sendNavigationRequest(vin: String, destination: String): Result<Unit> {
                navDest += destination
                return if (navOk) Result.success(Unit) else Result.failure(IllegalStateException("nav fail"))
            }

            override suspend fun wakeVehicle(vin: String): Result<Unit> =
                Result.failure(UnsupportedOperationException())
            override suspend fun sendVehicleCommand(
                vin: String,
                commandName: String,
                whichTrunk: String?,
                jsonBody: String?,
            ): Result<Unit> = Result.failure(UnsupportedOperationException())
        }
        val settings = object : SettingsRepository by ThrowingSettings {
            override suspend fun getVin(): String? = "TESTVIN1234567890"
        }
        val history = object : HistoryRepository by ThrowingHistory {
            override suspend fun fleetEvents(filter: HistoryFilterState) = listOf(
                FleetApiHistoryItem(
                    id = "1",
                    atMs = 1L,
                    category = "info",
                    ok = true,
                    detail = "최근 알림 본문",
                ),
            )
        }
        return VoiceCommandUseCase(
            speech = speech,
            communications = comms,
            tts = tts,
            voiceNavUseCase = VoiceNavUseCase(fleet, settings, speech),
            settingsRepository = settings,
            historyRepository = history,
            debugLogger = DebugLogger(TestSettings()),
        )
    }
}

private object ThrowingSettings : SettingsRepository {
    override suspend fun getVin(): String? = error("unused")
    override suspend fun setVin(vin: String) = error("unused")
    override suspend fun getSpeedUnitKmh(): Boolean = error("unused")
    override suspend fun setSpeedUnitKmh(useKmh: Boolean) = error("unused")
    override suspend fun isOnboardingComplete(): Boolean = error("unused")
    override suspend fun setOnboardingComplete(complete: Boolean) = error("unused")
    override suspend fun getGaugeDisplayPrefs() = error("unused")
    override suspend fun setGaugeDisplayPrefs(prefs: com.myt.domain.model.GaugeDisplayPrefs) = error("unused")
    override suspend fun isDarkTheme(): Boolean = error("unused")
    override suspend fun setDarkTheme(enabled: Boolean) = error("unused")
    override suspend fun isDriveSafetyAcknowledged(): Boolean = error("unused")
    override suspend fun setDriveSafetyAcknowledged(acknowledged: Boolean) = error("unused")
}

private object ThrowingHistory : HistoryRepository {
    override suspend fun recordTrip(item: TripHistoryItem) = error("unused")
    override suspend fun updateTripEnd(item: TripHistoryItem) = error("unused")
    override suspend fun recordCharge(item: ChargeHistoryItem) = error("unused")
    override suspend fun recordFleetEvent(category: FleetCallCategory, ok: Boolean, detail: String?) = error("unused")
    override suspend fun saveVehicleSnapshot(vin: String, state: GaugeState) = error("unused")
    override suspend fun loadVehicleSnapshot(vin: String): GaugeState? = null
    override suspend fun trips(filter: HistoryFilterState): List<TripHistoryItem> = emptyList()
    override suspend fun tripById(id: String): TripHistoryItem? = null
    override suspend fun chargeSessions(filter: HistoryFilterState): List<ChargeHistoryItem> = emptyList()
    override suspend fun fleetEvents(filter: HistoryFilterState): List<FleetApiHistoryItem> = emptyList()
}
