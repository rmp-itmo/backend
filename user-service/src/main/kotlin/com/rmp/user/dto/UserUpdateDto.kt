package com.rmp.user.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateDto(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val activityType: String? = null,
    val goalType: String? = null,
    val isMale: Boolean? = null,
    val age: Int? = null,
    val nickname: String? = null,
    val date: Int
): SerializableClass