package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: Long,
    val authorId: Long,
    val authorNickname: String,
    val authorIsMale: Boolean,
    val upVotes: Int,
    val image: String?,
    val text: String?,
    val title: String,
): SerializableClass
