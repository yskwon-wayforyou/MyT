package com.myt.domain.usecase

import com.myt.domain.model.CameraType
import com.myt.domain.model.SpeedCamera
import com.myt.domain.repository.PoiRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class RoadSnapUseCaseTest {
    @Test
    fun fallsBackToLocalCameraProjectionWhenOsrmFails() = runBlocking {
        val engine = MockEngine {
            respond(
                content = "error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }
        val poi = object : PoiRepository {
            override fun findNearbyCameras(lat: Double, lng: Double, radiusM: Int): List<SpeedCamera> =
                listOf(
                    SpeedCamera("a", 37.0, 127.0, 60, "road", 0f, CameraType.FIXED),
                    SpeedCamera("b", 37.001, 127.001, 60, "road", 0f, CameraType.FIXED),
                )
        }
        val useCase = RoadSnapUseCase(HttpClient(engine), poi)
        val snapped = useCase.snap(37.0005, 127.0005)
        // Should be somewhere between the two camera points, not the error path crash.
        assertEquals(true, snapped.first in 37.0..37.001)
        assertEquals(true, snapped.second in 127.0..127.001)
    }
}
