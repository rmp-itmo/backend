package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class DoubleRecordException (
    override val message: String
): BaseException(409, "Double record", message)