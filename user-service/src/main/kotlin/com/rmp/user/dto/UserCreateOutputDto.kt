package com.rmp.user.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserCreateOutputDto(
    val id: Long
): SerializableClass