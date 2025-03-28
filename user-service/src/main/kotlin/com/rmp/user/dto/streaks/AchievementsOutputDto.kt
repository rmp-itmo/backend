package com.rmp.user.dto.streaks

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class AchievementsOutputDto (
    val calories: AchievementDto,
    val water: AchievementDto,
    val steps: AchievementDto,
    val sleep: AchievementDto,
): SerializableClass