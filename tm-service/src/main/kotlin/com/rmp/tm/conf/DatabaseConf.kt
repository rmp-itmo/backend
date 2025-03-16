package com.rmp.tm.conf

data class DatabaseConf(
    val proto: String,
    val url: String,
    val dbName: String,
    val driverClassName: String,
    val username: String,
    val password: String,
) {
    val jdbcUrl: String
        get() = "$proto://$url/$dbName"
}