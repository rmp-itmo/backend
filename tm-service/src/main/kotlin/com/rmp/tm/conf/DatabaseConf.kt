package com.rmp.tm.conf

data class DatabaseConf(
    val jdbcUrl: String,
    val driverClassName: String,
    val username: String,
    val password: String,
)