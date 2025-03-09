package com.rmp.auth.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserOutputDto(
    val id: Long,
    val name: String,
    val login: String,
    val lastLogin: Long
): SerializableClass
