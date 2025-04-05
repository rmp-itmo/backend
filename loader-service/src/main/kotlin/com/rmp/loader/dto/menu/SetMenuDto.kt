package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetMenuDto(
    @SerialName("meals")
    val meals: List<Meal>,
    @SerialName("params")
    val params: ParamsDto
) {
    @Serializable
    data class Meal(
        @SerialName("dishes")
        val dishes: List<Int>,
        @SerialName("name")
        val name: String
    )
}