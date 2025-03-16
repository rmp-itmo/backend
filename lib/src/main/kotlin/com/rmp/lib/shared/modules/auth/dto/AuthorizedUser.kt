package com.rmp.lib.shared.modules.auth.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class AuthorizedUser(
    val id: Long,
    val lastLogin: Long? = null
): SerializableClass
