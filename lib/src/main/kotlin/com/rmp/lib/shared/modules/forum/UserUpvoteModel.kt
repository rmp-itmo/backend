package com.rmp.lib.shared.modules.forum

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object UserUpvoteModel: IdTable("user_upvote") {
    val userId = reference("user_id", UserModel)
    val postId = reference("post_id", PostModel)
}