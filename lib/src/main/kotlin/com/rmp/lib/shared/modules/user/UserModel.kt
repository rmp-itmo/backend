package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserModel: IdTable("user_model") {
    val name = text("name")

    val login = text("login")
    val password = text("password")
}