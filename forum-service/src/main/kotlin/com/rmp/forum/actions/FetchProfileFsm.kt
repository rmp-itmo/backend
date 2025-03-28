package com.rmp.forum.actions

import com.rmp.forum.services.ProfileService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class FetchProfileFsm(di: DI) : Fsm("fetch-profile", di) {
    private val profileService: ProfileService by instance()

    enum class FetchProfileEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(FetchProfileEventState.INIT) {
            profileService.getProfile(this)
        }
    }
}