package com.rmp.auth.actions

import com.rmp.auth.services.AuthService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class AuthFsm(event: String, di: DI) : Fsm(event, di) {
    private val authService by instance<AuthService>()

    override fun Fsm.registerStates() {
        on(AuthEventState.INIT) {
            authService.fetchUser(this)
        }

        on(AuthEventState.VERIFY) {
            authService.verify(this)
        }
    }
}