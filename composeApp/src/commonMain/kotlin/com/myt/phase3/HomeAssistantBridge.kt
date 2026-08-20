package com.myt.phase3

/** M39 — Home Assistant MQTT integration (Phase 3 stub). */
interface HomeAssistantBridge {
    suspend fun publishState(topic: String, payload: String)
}

class StubHomeAssistantBridge : HomeAssistantBridge {
    override suspend fun publishState(topic: String, payload: String) = Unit
}
