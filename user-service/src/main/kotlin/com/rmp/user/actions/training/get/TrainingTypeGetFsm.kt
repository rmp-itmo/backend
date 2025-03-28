package com.rmp.user.actions.training.get

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.TrainingService
import org.kodein.di.DI
import org.kodein.di.instance

class TrainingTypeGetFsm(di: DI): Fsm("user-get-training-types", di) {
    private val trainingService: TrainingService by instance()

    enum class TrainingTypeGetEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(TrainingTypeGetEventState.INIT) {
            trainingService.getTypes(this)
        }
    }
}