package com.rmp.api.modules.diet

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kodein.di.DI


class UserController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("users") {
            route("log") {
                post("water") {
                    call.process("user-upload-water-log", AppConf.redis.diet)
                }

                post("dish") {
                    call.process("user-upload-dish-log", AppConf.redis.diet)
                }
            }
        }
    }
}