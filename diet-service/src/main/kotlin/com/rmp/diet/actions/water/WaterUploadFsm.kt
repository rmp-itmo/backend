package com.rmp.diet.actions.water

import com.rmp.diet.services.DietUploadService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class WaterUploadFsm(event: String, di: DI) : Fsm(event, di) {
    private val dietUploadService: DietUploadService by instance()

    override fun Fsm.registerStates() {
        any {
            dietUploadService.uploadWater(this)
        }
    }
}