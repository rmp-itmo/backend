package com.rmp.lib.shared.modules.stat

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object GraphCacheModel: IdTable("graph_cache") {
    val name = text("graph_name")
    val userId = reference("user_id", UserModel)
    val top = int("top_bound")
    val bottom = int("bottom_bound")
    val results = text("results")
}