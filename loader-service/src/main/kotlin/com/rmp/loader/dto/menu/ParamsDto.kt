package com.rmp.loader.dto.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ParamsDto(
    @SerialName("calories")
    val calories: Double,
    @SerialName("carbohydrates")
    val carbohydrates: Double,
    @SerialName("fat")
    val fat: Double,
    @SerialName("protein")
    val protein: Double
)
