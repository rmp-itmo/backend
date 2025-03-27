package com.rmp.diet.actions.water.get

import com.rmp.diet.services.DietLogService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class WaterGetHistoryFsm(di: DI) : Fsm("water-history", di) {
    private val dietLogService: DietLogService by instance()

    override fun Fsm.registerStates() {
        on(WaterGetHistoryEventState.INIT) {
            dietLogService.getWaterHistory(this)
        }
    }
}