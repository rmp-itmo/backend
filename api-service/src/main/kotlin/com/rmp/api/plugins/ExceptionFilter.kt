package com.rmp.api.plugins

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.exceptions.BaseException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.utils.Logger
import com.rmp.lib.exceptions.BadRequestException as BadRequestExceptionLocal
import io.ktor.server.plugins.BadRequestException as BadRequestExceptionKtor

fun Application.configureExceptionFilter() {

    install(StatusPages) {
        fun Throwable.getClientMessage(): String = if (AppConf.isDebug) message.toString() else ""

        exception<Throwable> {
            call, cause ->
//                Logger.callFailed(call, cause)
//                rollbackAndClose()
                call.respond<InternalServerException>(
                    HttpStatusCode.InternalServerError,
                    InternalServerException(cause.getClientMessage())
                )
        }

        exception<NoTransformationFoundException> {
            call, requestValidationException ->
//                Logger.callFailed(call, requestValidationException)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = InternalServerException(requestValidationException.getClientMessage())
                )
        }

        exception<BadRequestExceptionKtor> {
            call, requestValidationException ->
//                Logger.callFailed(call, requestValidationException)
                call.respond(
                    status = HttpStatusCode.UnsupportedMediaType,
                    message = BadRequestExceptionLocal(requestValidationException.getClientMessage())
                )
        }

        exception<BaseException> {
            call, cause ->
//                Logger.callFailed(call, cause)
                call.respond(
                    status = HttpStatusCode(cause.httpStatusCode, cause.httpStatusText),
                    cause
                )
        }
    }
}
