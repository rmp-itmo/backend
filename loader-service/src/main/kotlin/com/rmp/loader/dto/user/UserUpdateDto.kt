package com.rmp.loader.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserUpdateDto(
    @SerialName("name")
    val name: String,
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("height")
    val height: Float,
    @SerialName("weight")
    val weight: Float,
    @SerialName("activityType")
    val activityType: String,
    @SerialName("goalType")
    val goalType: String,
    @SerialName("isMale")
    val isMale: Boolean,
    @SerialName("age")
    val age: Int,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("date")
    val date: Int
)