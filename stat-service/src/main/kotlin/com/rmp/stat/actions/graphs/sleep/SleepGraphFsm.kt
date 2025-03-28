package com.rmp.stat.actions.graphs.sleep

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.stat.services.GraphService
import org.kodein.di.DI
import org.kodein.di.instance

class SleepGraphFsm(di: DI) : Fsm("fetch-sleep-graph", di) {
    private val graphService: GraphService by instance()

    override fun Fsm.registerStates() {
        on(SleepGraphEventState.INIT) {
            graphService.buildSleepGraph(this)
        }
    }
}