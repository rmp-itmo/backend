package com.rmp.loader.dto.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostCreateDto(
    @SerialName("title")
    val title: String,
    @SerialName("text")
    val text: String
)