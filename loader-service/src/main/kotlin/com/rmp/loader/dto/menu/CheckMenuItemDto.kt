package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckMenuItemDto(
    @SerialName("check")
    val check: Boolean,
    @SerialName("menuItemId")
    val menuItemId: Int
)