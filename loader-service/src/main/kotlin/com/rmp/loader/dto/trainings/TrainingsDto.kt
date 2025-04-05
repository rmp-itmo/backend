package com.rmp.loader.dto.trainings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainingsDto(
    @SerialName("trainings")
    val trainings:  Map<Int, List<TrainingDto>>
) {
    @Serializable
    data class TrainingDto (
        @SerialName("id")
        val id: Long,
        @SerialName("start")
        val start: String,
        @SerialName("end")
        val end: String,
        @SerialName("calories")
        val calories: Double,
        @SerialName("type")
        val type: String,
        @SerialName("intensity")
        val intensity: String,
    )
}