package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class DuplicateEntityException(
    override val message: String
): BaseException(409, "Duplicate entity", message)