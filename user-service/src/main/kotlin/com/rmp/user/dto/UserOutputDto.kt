package com.rmp.user.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserOutputDto(
    val id: Long,
    val name: String,
    val email: String,
    val height: Float,
    val weight: Float,
    val activityType: String,
    val goalType: String,
    val isMale: Boolean,
    val age: Int,
    val waterTarget: Double,
    val caloriesTarget: Double,
    val waterStreak: Int,
    val caloriesStreak: Int,
    val nickName: String,
    val stepsTarget: Int,
    val stepsCount: Int,
    val heartRate: Int?,
    val waterCurrent: Double,
    val caloriesCurrent: Double,
    val waterCoefficient: Float? = null,
    val caloriesCoefficient: Float? = null,
    val goalCoefficient: Float? = null,
): SerializableClass