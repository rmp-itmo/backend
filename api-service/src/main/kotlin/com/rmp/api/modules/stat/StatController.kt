package com.rmp.api.modules.stat

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class StatController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("stat") {
            route("graph") {
                authenticate("default") {
                    post("heart") {
                        call.process("fetch-heart-graph", AppConf.redis.stat)
                    }

                    post("sleep") {
                        call.process("fetch-sleep-graph", AppConf.redis.stat)
                    }
                }
            }
        }
    }
}