package com.myt.domain.bluetooth

/**
 * Tesla Phone Key / BLE advertisement matching.
 * Local name format (docs): `S` + first 8 hex of SHA1(VIN) + `C`.
 * Also matches legacy scan names and VCSEC service UUID.
 */
object TeslaBlePresence {
    /** VCSEC / Phone Key GATT service UUID (Tesla vehicle-command protocol). */
    const val VCSEC_SERVICE_UUID = "00000211-b2d1-43f0-9b88-960cebf8b91e"

    private val phoneKeyName = Regex("^S[0-9A-Fa-f]{8}C$")

    fun matchesAdvertisementName(name: String?): Boolean {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return false
        if (phoneKeyName.matches(n)) return true
        if (n.contains("Tesla", ignoreCase = true)) return true
        if (n.contains("Sentry", ignoreCase = true)) return true
        if (n.startsWith("TI", ignoreCase = true) && n.length <= 12) return true
        return false
    }

    fun matchesServiceUuid(uuid: String?): Boolean {
        if (uuid.isNullOrBlank()) return false
        return uuid.equals(VCSEC_SERVICE_UUID, ignoreCase = true) ||
            uuid.contains("0211-b2d1-43f0", ignoreCase = true)
    }
}
