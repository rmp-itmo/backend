package com.rmp.loader.dto.hello

import kotlinx.serialization.Serializable

@Serializable
data class SignupDto(
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
)
