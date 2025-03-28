package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostDto (
    val title: String,
    val text: String? = null,
    val imageName: String? = null,
    val image: String? = null
): SerializableClass