package com.xdd.pantry.bootstrap.telegram

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(val botToken: String = "")
