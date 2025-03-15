package com.rmp.diet.dto.target

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetCheckInputDto(
    val timestamp: Long
): SerializableClass