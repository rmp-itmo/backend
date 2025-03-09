package com.rmp.auth.actions.auth

import com.rmp.auth.services.AuthService
import com.rmp.auth.services.RefreshService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class AuthFsm(event: String, di: DI) : Fsm(event, di) {
    private val authService by instance<AuthService>()
    private val refreshService by instance<RefreshService>()

    override fun Fsm.registerStates() {
        on(AuthEventState.INIT) {
            authService.fetchUser(this)
        }

        on(AuthEventState.VERIFY) {
            authService.verify(this)
        }

        on(AuthEventState.UPDATED) {
            refreshService.updated(this)
        }
    }
}