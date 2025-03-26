package com.rmp.user.actions.update.steps

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.instance
import org.kodein.di.DI

class UserStepsUpdateFsm(di: DI): Fsm("update-user-steps", di) {
    private val userService: UserService by instance()

    override fun Fsm.registerStates() {
        on(UserStepsUpdateEventState.INIT) {
            userService.updateSteps(this)
        }
    }
}