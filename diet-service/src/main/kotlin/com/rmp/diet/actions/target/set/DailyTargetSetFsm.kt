package com.rmp.diet.actions.target.set

import com.rmp.diet.services.DailyTargetService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DailyTargetSetFsm(di: DI): Fsm("user-daily-target-set", di) {
    private val dailyTargetSetService: DailyTargetService by instance()

    override fun Fsm.registerStates() {
        on(DailyTargetSetEventState.INIT) {
            dailyTargetSetService.set(this)
        }
    }
}