package com.myt.platform

expect class OAuthPlatform {
    fun openAuthorizationUrl(url: String)
}
