package com.rmp.auth.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TokenOutputDto (
    val accessToken: String,
    val refreshToken: String
): SerializableClass