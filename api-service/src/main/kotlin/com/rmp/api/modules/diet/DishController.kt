package com.rmp.api.modules.diet

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class DishController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("dish") {
            authenticate("default") {
                post("find") {
                    call.process("dish-fetch", AppConf.redis.diet)
                }
            }
        }
    }
}