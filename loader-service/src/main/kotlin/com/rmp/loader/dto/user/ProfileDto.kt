package com.rmp.loader.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto (
    @SerialName("id")
    val id: Long,
    @SerialName("nickName")
    val nickName: String,
    @SerialName("subsNum")
    val subsNum: Long,
    @SerialName("subscriptions")
    val subscriptions: Map<Long, String>,
    @SerialName("registrationDate")
    val registrationDate: Int,
    @SerialName("posts")
    val posts: List<Post>
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