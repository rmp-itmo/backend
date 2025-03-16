package com.rmp.auth.actions.getme

import com.rmp.auth.services.GetMeService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class GetMeFsm(di: DI) : Fsm("getme", di) {
    private val getMeService: GetMeService by instance()

    override fun Fsm.registerStates() {
        on(GetMeEventState.INIT) {
            getMeService.fetchUser(this)
        }
    }
}