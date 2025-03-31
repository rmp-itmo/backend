package com.rmp.user.dto.sleep

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSleepHistory (
    val sleepTarget: Float,
    val from: Int,
    val to: Int,
    val data: List<UserSleepDto>
): SerializableClass