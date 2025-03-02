package com.rmp.auth.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class AuthInputDto (
    val login: String,
    val password: String
): SerializableClass