package com.rmp.user.actions.get

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.instance
import org.kodein.di.DI

class UserGetFsm(di: DI): Fsm("get-user", di) {
    private val userService: UserService by instance()

    override fun Fsm.registerStates() {
        on(UserGetEventState.INIT) {
            userService.getUser(this)
        }
    }
}