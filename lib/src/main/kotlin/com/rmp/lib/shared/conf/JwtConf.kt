package com.rmp.lib.shared.conf

data class JwtConf (
    val domain: String,
    val secret: String,
    val expirationTime: Int,
    val refreshExpirationTime: Int,
)