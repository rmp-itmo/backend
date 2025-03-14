package com.rmp.lib.exceptions

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

/* IF ADD ANY NEW FIELD, REMEMBER TO ADD IT TO AnyException TOO */

@Serializable
sealed class BaseException (
    open val httpStatusCode: Int,
    open val httpStatusText: String,
    open var data: String? = null
): Exception(data), SerializableClass
