package com.rmp.loader.dto.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpvoteDto(
    @SerialName("id")
    val id: Long,
    @SerialName("upvote")
    val upvote: Boolean = true
)