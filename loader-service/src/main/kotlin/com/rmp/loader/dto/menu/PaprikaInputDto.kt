package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaprikaInputDto(
    @SerialName("calories")
    val calories: Int,
    @SerialName("meals")
    val meals: List<Meal>
) {
    @Serializable
    data class Meal(
        @SerialName("name")
        val name: String,
        @SerialName("size")
        val size: Double,
        @SerialName("type")
        val type: Int
    )
}