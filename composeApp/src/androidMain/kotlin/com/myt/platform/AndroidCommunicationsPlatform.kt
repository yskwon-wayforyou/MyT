package com.myt.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

actual class DeviceCommunicationsPlatform actual constructor(context: Any) {
    private val ctx = context as Context

    actual fun dialPhone(number: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.filter { it.isDigit() || it == '+' }}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(ctx, intent, null)
    }

    actual fun sendSms(number: String, message: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${number.filter { it.isDigit() || it == '+' }}"))
            .putExtra("sms_body", message)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(ctx, intent, null)
    }

    actual fun shareKakaoTalk(message: String): Result<Unit> = runCatching {
        val kakao = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.kakao.talk")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (kakao.resolveActivity(ctx.packageManager) != null) {
            ContextCompat.startActivity(ctx, kakao, null)
        } else {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ContextCompat.startActivity(ctx, Intent.createChooser(fallback, "메시지 보내기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)
        }
    }

    actual fun openMessagingApp(): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ContextCompat.startActivity(ctx, intent, null)
    }
}
