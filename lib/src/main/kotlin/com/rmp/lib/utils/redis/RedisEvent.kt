package com.rmp.lib.utils.redis

import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.query.QueryResult
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import kotlinx.serialization.Serializable

@Serializable
data class RedisEvent (
    val action: String,
    var from: String,
    val eventType: String,
    val eventState: RedisEventState,
    val data: String,
    var tid: String? = null,
): SerializableClass {
    inline fun <reified T: SerializableClass> switchData(data: T, tid: String? = null): RedisEvent {
        if (!data::class.annotations.any {
            it.annotationClass == Serializable::class
        })
            throw Exception("Data must be serializable")
        return RedisEvent(action, from, eventType, eventState, Json.serializer.encodeToString(data), tid)
    }

    fun switch(stateMutation: RedisEventState): RedisEvent {
        return RedisEvent(action, from, eventType, stateMutation, data, tid)
    }

    fun mutateState(newState: Enum<*>) = eventState.mutate(newState)

    inline fun <reified T: SerializableClass> mutateState(newStateData: T) = eventState.mutate(newStateData)

    inline fun <reified T: SerializableClass> mutateState(newState: Enum<*>, newStateData: T) = eventState.mutate(newState.name, newStateData)

    inline fun <reified T: SerializableClass> parseData(silent: Boolean = false): T? =
        try {
            Json.serializer.decodeFromString<T>(data)
        } catch (e: Exception) {
            if (!silent)
                Logger.debugException("Data parsing failed for event $this", e, "main")
            null
        }

    inline fun <reified T: SerializableClass> parseState(silent: Boolean = false): T? =
        if (eventState.stateData != null)
            try {
                Json.serializer.decodeFromString<T>(eventState.stateData)
            } catch (e: Exception) {
                if (!silent)
                    Logger.debugException("State parsing failed for event $this", e, "main")
                null
            }
        else null

    fun parseDbSelect(): Map<String, List<Row>> {
        val queryResult = parseData<QueryResult>()

        return queryResult?.result?.mapNotNull { (label, result) ->
            if (result.parseData.isEmpty()) null
            else {
                label to result.rows.map {
                    Row.build(it, TableRegister.getColumns(result.parseData))
                }
            }
        }?.toMap() ?: mutableMapOf()
    }
}