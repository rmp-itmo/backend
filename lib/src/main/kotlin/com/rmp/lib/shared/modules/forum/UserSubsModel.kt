package com.rmp.lib.shared.modules.forum

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object UserSubsModel: IdTable("user_subs_model") {
    val userId = long("author_id")
    val sub = reference("user_id", UserModel)
}