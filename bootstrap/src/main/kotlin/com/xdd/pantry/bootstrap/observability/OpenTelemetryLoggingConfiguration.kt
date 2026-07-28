package com.xdd.pantry.bootstrap.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class OpenTelemetryLoggingConfiguration(openTelemetry: OpenTelemetry) {

    init {
        runCatching { OpenTelemetryAppender.install(openTelemetry) }
            .onFailure { failure -> log.warn("Failed to install the OpenTelemetry Logback appender", failure) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OpenTelemetryLoggingConfiguration::class.java)
    }
}
