package com.rmp.api.modules.diet

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class ServiceController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("service-dish"){
            post("create") {
                call.processUnauthorized("service-dish-create", AppConf.redis.diet)
            }
            get("all") {
                call.processUnauthorized("service-dish-get-all", AppConf.redis.diet)
            }
        }
    }
}