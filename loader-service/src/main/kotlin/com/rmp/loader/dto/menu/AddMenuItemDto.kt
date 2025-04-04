package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddMenuItemDto(
    @SerialName("check")
    val check: Boolean,
    @SerialName("dishId")
    val dishId: Int,
    @SerialName("mealId")
    val mealId: Long
)