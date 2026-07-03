package com.xdd.pantry.bootstrap.web

import com.xdd.pantry.application.users.RegisterUserUseCase
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUser

@Component
class CurrentUserArgumentResolver(
    private val auth: TelegramInitDataAuth,
    private val registerUser: RegisterUserUseCase,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) && parameter.parameterType == UUID::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UUID {
        val header = webRequest.getHeader(HttpHeaders.AUTHORIZATION)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header")
        val telegramUserId = try {
            auth.verify(header.removePrefix("tma").trim())
        } catch (invalid: InvalidInitDataException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, invalid.message)
        }
        return registerUser.findOrRegister(telegramUserId).id.value
    }
}
