package com.rmp.api.plugins

import com.rmp.api.utils.api.ExceptionResponseDto
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.exceptions.BaseException
import io.ktor.server.plugins.BadRequestException as BadRequestExceptionKtor

fun Application.configureExceptionFilter() {

    install(StatusPages) {
        fun Throwable.getClientMessage(): String = if (AppConf.isDebug) message.toString() else ""

        exception<Throwable> {
            call, cause ->
//                Logger.callFailed(call, cause)
//                rollbackAndClose()
                call.respond<ExceptionResponseDto>(
                    HttpStatusCode.InternalServerError,
                    ExceptionResponseDto(500, "Internal server error", cause.getClientMessage())
                )
        }

        exception<NoTransformationFoundException> {
            call, requestValidationException ->
//                Logger.callFailed(call, requestValidationException)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ExceptionResponseDto(500, "Internal server error", requestValidationException.getClientMessage())
                )
        }

        exception<BadRequestExceptionKtor> {
            call, requestValidationException ->
//                Logger.callFailed(call, requestValidationException)
                call.respond(
                    status = HttpStatusCode.UnsupportedMediaType,
                    message = ExceptionResponseDto(400, "Bad request", requestValidationException.getClientMessage())
                )
        }

        exception<BaseException> {
            call, cause ->
//                Logger.callFailed(call, cause)
                call.respond(
                    status = HttpStatusCode(cause.httpStatusCode, cause.httpStatusText),
                    ExceptionResponseDto(cause.httpStatusCode, cause.httpStatusText, cause.message ?: cause.httpStatusText)
                )
        }
    }
}
