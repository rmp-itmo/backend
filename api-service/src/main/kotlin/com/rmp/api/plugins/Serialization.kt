package com.rmp.api.plugins

import com.rmp.lib.utils.serialization.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json.serializer)
    }
}
