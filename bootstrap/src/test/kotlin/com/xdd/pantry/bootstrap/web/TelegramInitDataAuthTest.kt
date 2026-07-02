package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.bootstrap.telegram.TelegramProperties
import com.xdd.pantry.domain.users.TelegramUserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TelegramInitDataAuthTest {

    private val botToken = "123456:test-bot-token"
    private val auth = TelegramInitDataAuth(TelegramProperties(botToken), jacksonObjectMapper())

    @Test
    fun `accepts init data with a valid signature and extracts the user id`() {
        val initData = signedInitData(userId = 42, authDate = Instant.now().epochSecond)

        auth.verify(initData) shouldBe TelegramUserId(42)
    }

    @Test
    fun `rejects init data with a tampered signature`() {
        val tampered = signedInitData(userId = 42, authDate = Instant.now().epochSecond)
            .replaceAfterLast("hash=", "deadbeef")

        shouldThrow<InvalidInitDataException> { auth.verify(tampered) }
    }

    @Test
    fun `rejects expired init data`() {
        val stale = signedInitData(userId = 42, authDate = Instant.now().epochSecond - 100_000)

        shouldThrow<InvalidInitDataException> { auth.verify(stale) }
    }

    private fun signedInitData(userId: Long, authDate: Long): String {
        val fields = mapOf(
            "auth_date" to authDate.toString(),
            "user" to """{"id":$userId,"first_name":"Test"}""",
        )
        val dataCheckString = fields.toSortedMap().map { (k, v) -> "$k=$v" }.joinToString("\n")
        val secretKey = hmac("WebAppData".toByteArray(), botToken.toByteArray())
        val hash = hmac(secretKey, dataCheckString.toByteArray()).joinToString("") { "%02x".format(it) }
        return (fields + ("hash" to hash)).entries.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)
}
