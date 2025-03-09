package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserLoginModel: IdTable("user_login_model") {
    val user = reference("user_id", UserModel)
    val lastLogin = long("last_login")
}