package com.rmp.stat.actions.graphs.heart

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.stat.services.GraphService
import org.kodein.di.DI
import org.kodein.di.instance

class HeartGraphFsm(di: DI) : Fsm("fetch-heart-graph", di) {
    private val graphService: GraphService by instance()

    override fun Fsm.registerStates() {
        on(HeartGraphEventState.INIT) {
            graphService.buildHeartGraph(this)
        }
    }
}