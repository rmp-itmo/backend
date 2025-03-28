package com.rmp.api.modules.files

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.utils.files.FilesUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class FilesController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("files") {
            get("{fileName}") {
                val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.NotFound)
                val data = FilesUtil.read(fileName) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondBytes(data)
            }
        }

    }
}