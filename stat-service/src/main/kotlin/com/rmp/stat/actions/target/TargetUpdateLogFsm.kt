package com.rmp.stat.actions.target

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.stat.services.TargetService
import org.kodein.di.DI
import org.kodein.di.instance

class TargetUpdateLogFsm(di: DI): Fsm("target-update-log", di) {
    private val targetService: TargetService by instance()

    enum class TargetUpdateLogEventState {
        LOG
    }

    override fun Fsm.registerStates() {
        on(TargetUpdateLogEventState.LOG) {
            targetService.logTargets(this)
        }
    }
}