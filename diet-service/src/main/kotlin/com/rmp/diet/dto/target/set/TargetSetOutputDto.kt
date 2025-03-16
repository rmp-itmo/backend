package com.rmp.diet.dto.target.set

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetSetOutputDto(
    val status: String
): SerializableClass