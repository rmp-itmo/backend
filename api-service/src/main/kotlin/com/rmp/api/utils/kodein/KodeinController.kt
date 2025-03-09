package com.rmp.api.utils.kodein

import com.rmp.api.utils.ApiService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.kodein.di.DIAware
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.security.jwt.JwtUtil
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.*
import org.kodein.di.instance

abstract class KodeinController: DIAware {
    protected val apiService: ApiService by instance()

    abstract fun Route.registerRoutes()

    fun ApplicationCall.getAuthorized(): AuthorizedUser {
        val principal = principal<JWTPrincipal>() ?: throw ForbiddenException()
        return JwtUtil.decodeAccessToken(principal.payload)
    }

    suspend fun ApplicationCall.process(eventName: String, channel: String) {
        respond(apiService.process(eventName, receiveText(), channel, getAuthorized()))
    }

    suspend fun ApplicationCall.processUnauthorized(eventName: String, channel: String) {
        respond(apiService.process(eventName, receiveText(), channel, null))
    }

    suspend fun ApplicationCall.respond(deferred: CompletableDeferred<String>): Unit = withContext(Dispatchers.IO) {
        launch {
            val data = deferred.await()
            respondText(data, ContentType.Application.Json)
            return@launch
        }

        if (!deferred.isActive)
            return@withContext

        delay(10000)
        throw InternalServerException("Service down")
    }

    fun Parameters.getInt(name: String, errorMsg: String): Int {
        val param = this[name] ?: throw BadRequestException(errorMsg)
        return try {
            param.toInt()
        } catch (e: NumberFormatException) {
            throw BadRequestException(errorMsg)
        }
    }
}