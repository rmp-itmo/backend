package com.rmp.forum.actions

import com.rmp.forum.services.PostService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class ShareDishFsm(di: DI) : Fsm("share-dish", di) {
    private val postService: PostService by instance()

    enum class ShareDishEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(ShareDishEventState.INIT) {
            postService.shareMenu(this)
        }
    }
}