package com.rmp.lib.utils.redis

import com.rmp.lib.utils.serialization.Json
import kotlinx.serialization.Serializable

@Serializable
data class RedisEventState (
    val state: String,
    val stateData: String? = null
) {

    constructor(state: Enum<*>, stateData: String? = null) : this(state.name, stateData)

    enum class State {
        INIT, TERMINAL
    }

    fun mutate(newState: Enum<*>) = RedisEventState(newState, stateData)

    inline fun <reified T: SerializableClass> mutate(newStateData: T) =
        RedisEventState(state, Json.serializer.encodeToString(newStateData))

    inline fun <reified T: SerializableClass> mutate(newState: String, newStateData: T) =
        RedisEventState(newState, Json.serializer.encodeToString(newStateData))

    fun clearData(): RedisEventState {
        return RedisEventState(state, null)
    }
}