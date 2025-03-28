package com.rmp.user.actions.steps.log

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.StepsService
import org.kodein.di.DI
import org.kodein.di.instance

class StepsLogFsm(di: DI): Fsm("user-upload-steps-log", di) {
    private val stepsService: StepsService by instance()

    override fun Fsm.registerStates() {
        on(StepsLogEventState.INIT) {
            stepsService.logSteps(this)
        }
    }
}