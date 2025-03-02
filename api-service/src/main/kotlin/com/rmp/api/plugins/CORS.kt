package com.rmp.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
        allowHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
}