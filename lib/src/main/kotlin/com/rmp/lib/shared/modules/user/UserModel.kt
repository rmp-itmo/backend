package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.Table

object UserModel: Table("user_model") {
    val name = text("name")

    val login = text("login")
    val password = text("password")
}