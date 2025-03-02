package com.rmp.lib.shared.modules.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenDto (
    val id: Int,
    val lastLogin: Long
)