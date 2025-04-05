package com.rmp.loader.dto.achievement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AchievementsDto(
    @SerialName("calories")
    val calories: Achievement,
    @SerialName("water")
    val water: Achievement,
    @SerialName("steps")
    val steps: Achievement,
    @SerialName("sleep")
    val sleep: Achievement
) {
    @Serializable
    data class Achievement (
        @SerialName("current")
        val current: Int,
        @SerialName("max")
        val max: Int,
        @SerialName("percentage")
        val percentage: Int,
    )
}