package com.rmp.forum.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: Long,
    val nickName: String,
    val subsNum: Long,
    val subscriptions: Map<Long, String>,
    val registrationDate: Int,
    val posts: List<PostDto>,
    val isSubscribed: Boolean,
    val isMale: Boolean
): SerializableClass
