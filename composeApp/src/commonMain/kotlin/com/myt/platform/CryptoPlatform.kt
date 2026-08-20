package com.myt.platform

expect object CryptoPlatform {
    fun sha256(data: ByteArray): ByteArray
    fun secureRandomBytes(size: Int): ByteArray
}
