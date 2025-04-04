package com.rmp.loader.dto.heart


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddHeartLogDto(
    @SerialName("date")
    val date: Int,
    @SerialName("heartRate")
    val heartRate: Int,
    @SerialName("time")
    val time: Int
)