package com.rmp.user.actions.target

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.SleepService
import com.rmp.user.services.StepsService
import org.kodein.di.DI
import org.kodein.di.instance

class UserTargetCheckFsm(di: DI): Fsm("user-daily-target-check", di) {
    private val stepService: StepsService by instance()
    private val sleepService: SleepService by instance()

    override fun Fsm.registerStates() {
        on(UserTargetCheckEventState.CHECK_SLEEP) {
            sleepService.checkTarget(this)
        }

        on(UserTargetCheckEventState.CHECK_STEPS) {
            stepService.checkTarget(this)
        }
    }
}