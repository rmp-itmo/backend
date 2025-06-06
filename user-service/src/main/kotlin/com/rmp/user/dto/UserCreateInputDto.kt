package com.rmp.user.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserCreateInputDto(
    val name: String,
    val email: String,
    val password: String,
    val height: Float,
    val weight: Float,
    val activityType: Long,
    val goalType: Long,
    val isMale: Boolean,
    val age: Int,
    val registrationDate: Int,
): SerializableClass