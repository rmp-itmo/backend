package com.rmp.user.actions.create

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.instance
import org.kodein.di.DI

class UserCreateFsm(di: DI): Fsm("create-user", di) {
    private val userService: UserService by instance()

    override fun Fsm.registerStates() {
        on(UserCreateEventState.INIT) {
            userService.createUser(this)
        }
    }
}