package com.rmp.user.dto.steps

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class StepsDto(
    val count: Int
): SerializableClass