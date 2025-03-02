package com.rmp.api.modules.auth.controller

import com.rmp.api.modules.auth.service.ApiService
import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance

class AuthController(override val di: DI) : KodeinController() {
    private val apiService: ApiService by instance()
    /**
     * Method that subtypes must override to register the handled [Routing] routes.
     */
    override fun Route.registerRoutes() {
        route("auth") {
            post {
                val authInputDto = call.receiveText()
                val authResult = apiService.process("auth", authInputDto, AppConf.redis.auth)
                call.respond(authResult)
            }

            post("concurrent") {
                call.respondText("{\"id\": 123}", ContentType.Application.Json)
//                call.respond(authService.refresh(RefreshTokenDto(1, 1)))
            }
//            authenticate("default") {
//                post("refresh") {
//                    val refreshTokenDto = call.getRefresh()
//
//                    call.respond(authService.refresh(refreshTokenDto))
//                }
//                get("authorized") {
//                    call.respond(call.getAuthorized())
//                }
//            }
        }

    }
}