package com.rmp.loader.dto.water


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddWaterDto(
    @SerialName("date")
    val date: Int,
    @SerialName("time")
    val time: String,
    @SerialName("volume")
    val volume: Double
)