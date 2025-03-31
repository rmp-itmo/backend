package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class ShareAchievementInputDto (
    val achievementType: Int,
    val current: Int,
    val percentage: Int,
): SerializableClass