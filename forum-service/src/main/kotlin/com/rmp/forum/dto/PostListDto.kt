package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class PostListDto (
    val posts: List<PostDto>
): SerializableClass