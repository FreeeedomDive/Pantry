package com.xdd.pantry.bootstrap.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
import java.util.concurrent.TimeUnit

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            val route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: request.requestURI
            log.atInfo()
                .addKeyValue("http.request.method", request.method)
                .addKeyValue("http.route", route)
                .addKeyValue("http.response.status_code", response.status)
                .addKeyValue("http.server.request.duration_ms", millis)
                .log("{} {} -> {} in {} ms", request.method, route, response.status, millis)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith(ACTUATOR_PREFIX)

    companion object {
        private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
        private const val ACTUATOR_PREFIX = "/actuator"
    }
}
