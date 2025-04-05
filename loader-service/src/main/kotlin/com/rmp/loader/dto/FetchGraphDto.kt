package com.rmp.loader.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchGraphDto(
    @SerialName("day")
    val day: String? = null,
    @SerialName("month")
    val month: String? = null,
    @SerialName("year")
    val year: Int
)