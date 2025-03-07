package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.RedisEvent
import org.kodein.di.DI
import org.kodein.di.DIAware

class FsmRouter(override val di: DI) : DIAware {
    private val routes: MutableMap<String, Fsm> = mutableMapOf()

    companion object {
        fun routing(di: DI, configure: FsmRouter.() -> Unit): FsmRouter =
            FsmRouter(di).apply(configure)
    }

    fun fsm(eventType: String, configure: Fsm.() -> Unit) {
        routes += Pair(eventType, object: Fsm(eventType, di) {
            override fun Fsm.registerStates() {
                apply(configure)
            }
        })
    }

    fun fsm(fsm: Fsm) {
        routes += Pair(fsm.event, fsm.apply { registerStates() })
    }

    suspend fun process(event: RedisEvent) {
        val fsm = routes[event.eventType] ?: throw Exception("Unknown event type: ${event.eventType}")

        fsm.process(event)
    }
}