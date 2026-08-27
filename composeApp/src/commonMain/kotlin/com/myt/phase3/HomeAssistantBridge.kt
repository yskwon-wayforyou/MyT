package com.myt.phase3

/** M39 — Home Assistant integration bridge. */
interface HomeAssistantBridge {
    suspend fun publishState(topic: String, payload: String)
}
