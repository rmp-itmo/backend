package com.rmp.user.dto.heart

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class HeartLogOutputDto(
    val id: Long
): SerializableClass