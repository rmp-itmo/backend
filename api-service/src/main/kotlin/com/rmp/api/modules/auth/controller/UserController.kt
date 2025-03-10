package com.rmp.api.modules.auth.controller

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.kodein.di.DI


class UserController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("users") {
            route("upload") {
                post("water") {
                    val userUploadDto = call.receiveText()
                    val authorizedUser = call.getAuthorized()
                    val result = apiService.process("user-upload-water", userUploadDto, AppConf.redis.diet, authorizedUser)
                    call.respond(result)
                }

                post("intake") {
                    val userUploadDto = call.receiveText()
                    val authorizedUser = call.getAuthorized()
                    val result = apiService.process("user-upload-intake", userUploadDto, AppConf.redis.diet, authorizedUser)
                    call.respond(result)
                }
            }
        }
    }
}