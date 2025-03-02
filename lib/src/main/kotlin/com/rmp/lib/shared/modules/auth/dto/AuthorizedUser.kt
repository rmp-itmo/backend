package com.rmp.lib.shared.modules.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizedUser(
    val id: Int,
)
