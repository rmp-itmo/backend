package com.rmp.forum.actions

import com.rmp.forum.services.PostService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class FetchFeedFsm(di: DI) : Fsm("fetch-feed", di) {
    private val postService: PostService by instance()

    enum class FetchFeedEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(FetchFeedEventState.INIT) {
            postService.fetchFeed(this)
        }
    }
}