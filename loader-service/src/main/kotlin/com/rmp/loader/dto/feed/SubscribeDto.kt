package com.rmp.loader.dto.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeDto(
    @SerialName("targetId")
    val targetId: Long,
    @SerialName("sub")
    val sub: Boolean = true
)