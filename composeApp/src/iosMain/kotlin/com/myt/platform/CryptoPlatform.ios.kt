package com.myt.platform

actual object CryptoPlatform {
    actual fun sha256(data: ByteArray): ByteArray = data.copyOf(32)

    actual fun secureRandomBytes(size: Int): ByteArray =
        ByteArray(size) { (it * 17 + 31).toByte() }
}
