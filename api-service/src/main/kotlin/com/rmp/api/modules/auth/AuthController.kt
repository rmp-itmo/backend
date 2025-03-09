package com.rmp.api.modules.auth

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class AuthController(override val di: DI) : KodeinController() {
    /**
     * Method that subtypes must override to register the handled [Routing] routes.
     */
    override fun Route.registerRoutes() {
        route("auth") {
            post {
                call.processUnauthorized("auth", AppConf.redis.auth)
            }

            authenticate("default") {
                post("refresh") {
                    call.process("refresh", AppConf.redis.auth)
                }
                get("authorized") {
                    call.process("getme", AppConf.redis.auth)
                }
            }
        }

    }
}