package com.rmp.lib.utils.redis

import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.query.QueryResult
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DbResponseData(val value: Map<String, List<Row>>) {
    operator fun <T: Table> get(table: T): List<Row>? {
        return value[table.tableName_]
    }

    operator fun get(key: String): List<Row>? {
        return value[key]
    }
}

@OptIn(ExperimentalUuidApi::class)
object UuidSerializer : KSerializer<Uuid?> {
    override val descriptor: SerialDescriptor
        get() = Uuid.serializer().descriptor

    override fun deserialize(decoder: Decoder): Uuid? {
        return try {
            Uuid.serializer().deserialize(decoder)
        } catch (_: Exception) {null}
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Uuid?) {
        if (value == null) encoder.encodeNull()
        else Uuid.serializer().serialize(encoder, value)
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class RedisEvent (
    var action: String,
    var from: String,
    val eventType: String,
    val eventState: RedisEventState,
    val data: String,
    val authorizedUser: AuthorizedUser? = null,
    @Serializable(with = UuidSerializer::class)
    var tid: Uuid? = null,
    @Serializable(with = UuidSerializer::class)
    var dbRequest: Uuid? = null
): SerializableClass {
    inline fun <reified T: SerializableClass> mutateData(data: T): RedisEvent {
        if (!data::class.annotations.any {
                it.annotationClass == Serializable::class
            })
            throw Exception("Data must be serializable")
        return RedisEvent(
            action, from, eventType, eventState, Json.serializer.encodeToString(data), authorizedUser, tid, dbRequest
        )
    }

    fun mutate(stateMutation: RedisEventState): RedisEvent {
        return RedisEvent(
            action, from, eventType, stateMutation, data, authorizedUser, tid, dbRequest
        )
    }

    fun authorize(authorizedUser: AuthorizedUser?): RedisEvent {
        return RedisEvent(
            action, from, eventType, eventState, data, authorizedUser, tid, dbRequest
        )
    }

    fun mutate(newState: Enum<*>) = eventState.mutate(newState)

    inline fun <reified T: SerializableClass> mutate(newStateData: T) = eventState.mutate(newStateData)

    inline fun <reified T: SerializableClass> mutate(newState: Enum<*>, newStateData: T) = eventState.mutate(newState.name, newStateData)

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

    fun parseDb(): DbResponseData {
        val queryResult = parseData<QueryResult>()

        return (queryResult?.result?.mapNotNull { (label, result) ->
            if (result.parseData.isEmpty()) null
            else {
                label to result.rows.map {
                    Row.build(it, TableRegister.getColumns(result.parseData))
                }
            }
        }?.toMap() ?: mutableMapOf()).let {
            DbResponseData(it)
        }
    }

    fun forDb(requestId: Uuid): RedisEvent {
        return RedisEvent(action, from, eventType, eventState.clearData(), data, null, tid, requestId)
    }

    fun copyId(newEventType: String): RedisEvent =
        RedisEvent(action, from, newEventType, eventState, data, authorizedUser, tid)
}