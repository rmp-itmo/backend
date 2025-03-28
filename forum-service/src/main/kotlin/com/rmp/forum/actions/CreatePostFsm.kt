package com.rmp.forum.actions

import com.rmp.forum.services.PostService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class CreatePostFsm(di: DI) : Fsm("create-post", di) {
    private val postService: PostService by instance()

    enum class CreatePostEventState {
        INIT, UPLOAD_PHOTO
    }

    override fun Fsm.registerStates() {
        on(CreatePostEventState.INIT) {
            postService.createPost(this)
        }
    }
}