package com.rmp.user.actions.steps.update

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.StepsService
import org.kodein.di.instance
import org.kodein.di.DI

class UserStepsUpdateFsm(di: DI): Fsm("update-user-steps", di) {
    private val stepService: StepsService by instance()

    override fun Fsm.registerStates() {
        on(UserStepsUpdateEventState.INIT) {
            stepService.updateStepsTarget(this)
        }
    }
}