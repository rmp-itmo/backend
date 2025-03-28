package com.rmp.user.actions.update

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.instance
import org.kodein.di.DI

class UserUpdateFsm(di: DI): Fsm("update-user", di) {
    private val userService: UserService by instance()

    override fun Fsm.registerStates() {
        on(UserUpdateEventState.INIT) {
            userService.updateUser(this)
        }
    }
}