package com.rmp.api.utils.api

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class ExceptionResponseDto (
    val code: Int,
    val status: String,
    val message: String
): SerializableClass