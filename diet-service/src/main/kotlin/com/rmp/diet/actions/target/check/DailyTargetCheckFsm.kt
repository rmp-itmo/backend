package com.rmp.diet.actions.target.check

import com.rmp.diet.services.DailyTargetService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DailyTargetCheckFsm(di: DI): Fsm("user-daily-target-check", di) {
    private val dailyTargetService: DailyTargetService by instance()

    override fun Fsm.registerStates() {
        on(DailyTargetCheckEventState.INIT) {
            dailyTargetService.check(this)
        }
        on(DailyTargetCheckEventState.CHECK_DISH) {
            dailyTargetService.checkDishes(this)
        }
        on(DailyTargetCheckEventState.CHECK_WATER) {
            dailyTargetService.checkWater(this)
        }
        on(DailyTargetCheckEventState.UPDATE_STREAKS) {
            dailyTargetService.updateStreaks(this)
        }
    }
}