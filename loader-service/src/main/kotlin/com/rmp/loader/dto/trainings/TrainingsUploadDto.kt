package com.rmp.loader.dto.trainings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TrainingsUploadDto(
    @SerialName("type")
    val type: Long,
    @SerialName("intensity")
    val intensity: Long,
    // HHMM
    @SerialName("start")
    val start: Int,
    @SerialName("end")
    val end: Int,
    // YYYYMMDD
    @SerialName("date")
    val date: Int
)