package com.rmp.api.modules.logger

import com.rmp.api.utils.kodein.KodeinController
import io.ktor.server.routing.*
import org.kodein.di.DI

class LoggerController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("logger") {

        }
    }
}