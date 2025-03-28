package com.rmp.forum.actions

import com.rmp.forum.services.PostService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class UpvotePostFsm(di: DI) : Fsm("upvote-post", di) {
    private val postService: PostService by instance()

    enum class UpvotePostEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(UpvotePostEventState.INIT) {
            postService.upvotePost(this)
        }
    }
}