package com.rmp.lib.shared.conf

data class RedisConf (
    val host: String,
    val port: Int,

    val db: String,
    val api: String,
    val auth: String,
    val logger: String,
    val paprika: String,
    val diet: String,
)