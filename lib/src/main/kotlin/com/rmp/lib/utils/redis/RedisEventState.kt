package com.rmp.lib.utils.redis

import com.rmp.lib.utils.serialization.Json
import kotlinx.serialization.Serializable

@Serializable
data class RedisEventState (
    val state: String,
    val stateData: String? = null
) {
    fun mutate(newState: Enum<*>) = RedisEventState(newState.name, stateData)

    inline fun <reified T: SerializableClass> mutate(newStateData: T) = RedisEventState(state, Json.serializer.encodeToString(newStateData))

    inline fun <reified T: SerializableClass> mutate(newState: String, newStateData: T) = RedisEventState(newState, Json.serializer.encodeToString(newStateData))
}