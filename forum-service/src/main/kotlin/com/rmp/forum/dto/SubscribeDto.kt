package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeDto (
    val targetId: Long,
    val sub: Boolean = true
): SerializableClass