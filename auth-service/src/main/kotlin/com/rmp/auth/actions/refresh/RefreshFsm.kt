package com.rmp.auth.actions.refresh

import com.rmp.auth.services.RefreshService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class RefreshFsm(di: DI) : Fsm("refresh", di) {
    private val refreshService by instance<RefreshService>()

    override fun Fsm.registerStates() {
        on(RefreshEventState.INIT) {
            refreshService.fetchUser(this)
        }
    }
}