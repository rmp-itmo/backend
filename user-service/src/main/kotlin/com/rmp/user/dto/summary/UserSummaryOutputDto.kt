package com.rmp.user.dto.summary

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSummaryOutputDto(
    val caloriesTarget: Double,
    val caloriesCurrent: Double,
    val waterTarget: Double,
    val waterCurrent: Double,
    val stepsTarget: Int,
    val stepsCurrent: Int,
    val sleepHours: Int,
    val sleepMinutes: Int,
    val heartRate: Int?,
    val glassesOfWater: Double
): SerializableClass