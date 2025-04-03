package com.rmp.logger.models

import com.rmp.lib.utils.korm.Table
import java.time.LocalDateTime

object LogModel: Table("logger") {
    val type = text("type")
    val ts = dateTime("timestamp").default(LocalDateTime.now().toString().split(".").first())
    val labels = text("labels")
    val sender = text("sender")
    val message = text("message")
    val severity = text("severity")
    val cause = text("cause").nullable()
    val stackTrace = text("stackTrace").nullable()
    val actionId = text("actionId").nullable()
    val destination = text("destination").nullable()
}