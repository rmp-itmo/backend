package com.rmp.user.dto.heart

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
class HeartLogInputDto(
    val heartRate: Int,
    val date: Int,
    val time: Int,
): SerializableClass