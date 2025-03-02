package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class BadRequestException(
    override val message: String
) : BaseException(400, "Bad request", message)