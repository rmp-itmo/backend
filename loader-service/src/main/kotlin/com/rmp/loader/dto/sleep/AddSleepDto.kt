package com.rmp.loader.dto.sleep


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddSleepDto(
    @SerialName("date")
    val date: Int,
    @SerialName("hours")
    val hours: Int,
    @SerialName("minutes")
    val minutes: Int,
    @SerialName("quality")
    val quality: Int
)