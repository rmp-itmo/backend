package com.rmp.diet.actions.water

import com.rmp.diet.services.DietLogService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class WaterLogFsm(di: DI) : Fsm("user-upload-water-log", di) {
    private val dietLogService: DietLogService by instance()

    override fun Fsm.registerStates() {
        on(WaterLogEventState.INIT) {
            dietLogService.uploadWater(this)
        }
    }
}