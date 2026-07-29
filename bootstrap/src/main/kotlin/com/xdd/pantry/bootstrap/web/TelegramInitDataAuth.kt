package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.bootstrap.telegram.TelegramProperties
import com.xdd.pantry.domain.users.TelegramUserId
import com.xdd.pantry.infrastructure.telemetry.PantryTelemetry
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class InvalidInitDataException(message: String) : RuntimeException(message)

@Component
class TelegramInitDataAuth(
    private val properties: TelegramProperties,
    private val objectMapper: ObjectMapper,
    private val registry: MeterRegistry,
) {
    fun verify(initData: String): TelegramUserId {
        val params = runCatching { parse(initData) }
            .getOrElse { reject(PantryTelemetry.AuthFailureReason.MALFORMED, "malformed init data") }
        val hash = params["hash"] ?: reject(PantryTelemetry.AuthFailureReason.MISSING_HASH, "missing hash")
        val authDate = params["auth_date"]?.toLongOrNull()
            ?: reject(PantryTelemetry.AuthFailureReason.MISSING_AUTH_DATE, "missing auth_date")
        if (Instant.now().epochSecond - authDate > MAX_AGE_SECONDS) {
            reject(PantryTelemetry.AuthFailureReason.EXPIRED, "init data expired")
        }

        val dataCheckString = params.filterKeys { it != "hash" }
            .toSortedMap()
            .map { (key, value) -> "$key=$value" }
            .joinToString("\n")
        val secretKey = hmac("WebAppData".toByteArray(), properties.botToken.toByteArray())
        val expectedHash = hmac(secretKey, dataCheckString.toByteArray()).toHex()
        if (!expectedHash.equals(hash, ignoreCase = true)) {
            reject(PantryTelemetry.AuthFailureReason.BAD_SIGNATURE, "bad signature")
        }

        val userJson = params["user"] ?: reject(PantryTelemetry.AuthFailureReason.MISSING_USER, "missing user")
        return TelegramUserId(objectMapper.readValue<TelegramUser>(userJson).id)
    }

    private fun reject(reason: String, message: String): Nothing {
        registry.counter(PantryTelemetry.AUTH_FAILURES, PantryTelemetry.Tag.REASON, reason).increment()
        throw InvalidInitDataException(message)
    }

    private fun parse(initData: String): Map<String, String> =
        initData.split("&").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
        }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private data class TelegramUser(val id: Long)

    companion object {
        private const val MAX_AGE_SECONDS = 86_400L
    }
}
