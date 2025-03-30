package com.rmp.user.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserStepsUpdateDto(
    // YYYYMMDD
    val date: Int,
    val steps: Int
): SerializableClass