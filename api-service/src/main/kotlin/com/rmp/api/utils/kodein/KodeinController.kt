package com.rmp.api.utils.kodein

import com.rmp.api.utils.api.ApiService
import com.rmp.api.utils.api.ExceptionResponseDto
import com.rmp.lib.exceptions.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.kodein.di.DIAware
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.security.jwt.JwtUtil
import com.rmp.lib.utils.serialization.Json
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.*
import org.kodein.di.instance

abstract class KodeinController: DIAware {
    private val apiService: ApiService by instance()

    abstract fun Route.registerRoutes()

    private fun ApplicationCall.getAuthorized(): AuthorizedUser {
        val principal = principal<JWTPrincipal>() ?: throw ForbiddenException()
        return JwtUtil.decodeAccessToken(principal.payload)
    }

    suspend fun ApplicationCall.process(eventName: String, channel: String, timeout: Long = 10000) {
        respond(apiService.process(eventName, receiveText(), channel, getAuthorized()), timeout)
    }

    suspend fun ApplicationCall.processUnauthorized(eventName: String, channel: String, timeout: Long = 10000) {
        respond(apiService.process(eventName, receiveText(), channel, null), timeout)
    }

    private suspend fun ApplicationCall.respond(deferred: CompletableDeferred<String>, timeout: Long): Unit = withContext(Dispatchers.IO) {
        launch {
            val data = deferred.await()

            try {
                val ex = Json.serializer.decodeFromString<AnyException>(data)
                respond(HttpStatusCode.fromValue(ex.httpStatusCode), ExceptionResponseDto(ex.httpStatusCode, ex.httpStatusText, ex.message ?: ex.httpStatusText))
            } catch (ex: Exception) {
                respondText(data, ContentType.Application.Json)
            }

            return@launch
        }

        if (!deferred.isActive)
            return@withContext

        delay(timeout)
        throw InternalServerException("Service down")
    }
}