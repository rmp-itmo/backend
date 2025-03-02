package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.utils.redis.RedisEvent
import org.kodein.di.DI
import org.kodein.di.DIAware

abstract class Fsm(val event: String, override val di: DI): DIAware {
    private val states: MutableMap<String, suspend RedisEvent.() -> Unit> = mutableMapOf()

    fun on(state: Enum<*>, processor: suspend RedisEvent.() -> Unit) {
//        println(states)
        states += Pair(state.name, processor)
    }

    suspend fun process(event: RedisEvent) {
        val stateProcessor = states[event.eventState.state] ?: throw Exception("Unknown event state ${event.eventState.state}, available states is ${states.keys}")

        event.apply {
            stateProcessor(this)
        }
    }

    abstract fun Fsm.registerStates()
}