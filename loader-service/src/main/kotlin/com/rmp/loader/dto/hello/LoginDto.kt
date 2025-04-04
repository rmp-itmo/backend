package com.rmp.loader.dto.hello

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto (
    val login: String,
    val password: String
)