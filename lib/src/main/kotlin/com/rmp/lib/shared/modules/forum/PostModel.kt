package com.rmp.lib.shared.modules.forum

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object PostModel: IdTable("post_model") {
    val authorId = reference("author_id", UserModel)
    val title = text("title")
    val text = text("text").nullable().default(null)
    val image = text("image").nullable().default(null)
    val upVotes = int("up_votes").default(0)
    val timestamp = long("timestamp")
}