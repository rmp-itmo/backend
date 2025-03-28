package com.rmp.user.actions.training.log

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.TrainingService
import org.kodein.di.DI
import org.kodein.di.instance

class TrainingLogFsm(di: DI): Fsm("user-log-training", di) {
    private val trainingService: TrainingService by instance()

    enum class TrainingLogEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(TrainingLogEventState.INIT) {
            trainingService.logTraining(this)
        }
    }
}