package com.rmp.user.actions.sleep.set

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.SleepService
import org.kodein.di.DI
import org.kodein.di.instance

class SetSleepFsm(di: DI): Fsm("set-sleep", di) {
    private val sleepService: SleepService by instance()

    override fun Fsm.registerStates() {
        on(SetSleepEventState.INIT) {
            sleepService.setTodaySleep(this)
        }
    }
}