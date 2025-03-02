package com.rmp.lib.utils.serialization

import kotlinx.serialization.json.Json

object Json {
    val serializer = Json {
        ignoreUnknownKeys = true
    }
}