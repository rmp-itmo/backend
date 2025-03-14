package com.rmp.lib.shared.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class Response(val success: Boolean, val data: String): SerializableClass
