package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

/* !!!! ONLY FOR POLYMORPHIC SERIALIZATION DON'T TRY TO THROW IT !!!! */

@Serializable
data class AnyException (
    val httpStatusCode: Int,
    val httpStatusText: String,
    val data: String? = null,
    val message: String? = null
)