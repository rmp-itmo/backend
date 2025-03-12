package com.rmp.api.modules.paprika

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class PaprikaController(override val di: DI): KodeinController() {
    override fun Route.registerRoutes() {
        route("paprika") {
            authenticate("default") {
                post("calculate") {
                    call.process("generate-menu", AppConf.redis.paprika)
                }
            }
        }
    }
}