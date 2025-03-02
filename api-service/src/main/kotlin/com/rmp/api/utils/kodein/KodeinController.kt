package com.rmp.api.utils.kodein

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.kodein.di.DIAware
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.auth.dto.RefreshTokenDto
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.security.jwt.JwtUtil
import io.ktor.server.response.*
import kotlinx.coroutines.*

abstract class KodeinController: DIAware {

    abstract fun Route.registerRoutes()

    fun ApplicationCall.getAuthorized(): AuthorizedUser {
        val principal = principal<JWTPrincipal>() ?: throw ForbiddenException()
        return JwtUtil.decodeAccessToken(principal.payload)
    }

    fun ApplicationCall.getRefresh(): RefreshTokenDto {
        val principal = principal<JWTPrincipal>() ?: throw ForbiddenException()
        return JwtUtil.decodeRefreshToken(principal.payload)
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