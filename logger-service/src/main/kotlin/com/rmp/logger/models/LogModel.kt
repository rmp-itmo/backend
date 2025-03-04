package com.rmp.logger.models

import com.rmp.lib.utils.korm.Table

object LogModel: Table("logger") {
    val type = text("type")
    val ts = long("ts")
    val sender = text("sender")
    val prefix = text("prefix")
    val message = text("message")
    val severity = text("severity")
    val cause = text("cause").nullable()
    val stackTrace = text("stackTrace").nullable()
    val actionId = text("actionId").nullable()
    val destination = text("destination").nullable()
}