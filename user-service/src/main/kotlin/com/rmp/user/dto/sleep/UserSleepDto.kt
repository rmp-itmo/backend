package com.rmp.user.dto.sleep

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSleepDto (
    val id: Long,
    val userId: Long,
    val hours: Int,
    val minutes: Int,
    val date: Int,
): SerializableClass