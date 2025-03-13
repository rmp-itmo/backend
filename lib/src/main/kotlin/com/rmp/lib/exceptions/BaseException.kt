package com.rmp.lib.exceptions

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
sealed class BaseException (
    val httpStatusCode: Int,
    val httpStatusText: String,
    open var data: String? = null
): Exception(data), SerializableClass
