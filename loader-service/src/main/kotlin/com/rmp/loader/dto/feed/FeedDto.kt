package com.rmp.loader.dto.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedDto(
    @SerialName("posts")
    val posts: List<Post>,
) {
    @Serializable
    data class Post (
        @SerialName("id")
        val id: Long,
        @SerialName("authorId")
        val authorId: Long,
        @SerialName("authorNickname")
        val authorNickname: String,
        @SerialName("authorIsMale")
        val authorIsMale: Boolean,
        @SerialName("upVotes")
        val upVotes: Int,
        @SerialName("image")
        val image: String?,
        @SerialName("text")
        val text: String?,
        @SerialName("title")
        val title: String,
    )
}