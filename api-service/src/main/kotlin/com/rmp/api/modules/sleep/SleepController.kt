package com.rmp.api.modules.sleep

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class SleepController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("sleep") {
            authenticate("default") {
                post {
                    call.process("set-sleep", AppConf.redis.user)
                }
                post("history") {
                    call.process("fetch-sleep-history", AppConf.redis.user)
                }
            }
        }
    }
}