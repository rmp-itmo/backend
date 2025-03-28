package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UpvotePostDto (
    val id: Long,
    val upvote: Boolean = true
): SerializableClass