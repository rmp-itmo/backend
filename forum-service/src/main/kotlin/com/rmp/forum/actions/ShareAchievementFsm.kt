package com.rmp.forum.actions

import com.rmp.forum.services.PostService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class ShareAchievementFsm(di: DI) : Fsm("share-achievement", di, ) {
    private val postService: PostService by instance()

    enum class ShareAchievementEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(ShareAchievementEventState.INIT) {
            postService.shareAchievement(this)
        }
    }
}