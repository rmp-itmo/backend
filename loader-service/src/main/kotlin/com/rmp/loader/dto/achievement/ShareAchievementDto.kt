package com.rmp.loader.dto.achievement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShareAchievementDto(
    @SerialName("achievementType")
    val achievementType: Int,
    @SerialName("current")
    val current: Int,
    @SerialName("percentage")
    val percentage: Int,
)
