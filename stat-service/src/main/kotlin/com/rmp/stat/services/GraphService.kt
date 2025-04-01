package com.rmp.stat.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.stat.GraphCacheModel
import com.rmp.lib.shared.modules.user.UserHeartLogModel
import com.rmp.lib.shared.modules.user.UserSleepModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inRange
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.serialization.Json
import com.rmp.stat.dto.GraphConfigurationDto
import com.rmp.stat.dto.GraphOutputDto
import org.kodein.di.DI
import kotlin.math.max
import kotlin.math.min

class GraphService(di: DI) : FsmService(di, AppConf.redis.stat) {

    private suspend fun cache(redisEvent: RedisEvent, graphName: String, topBound: Int, bottomBound: Int, graphOutputDto: GraphOutputDto) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        newAutoCommitTransaction(redisEvent) {
            this add GraphCacheModel.insert {
                it[name] = graphName
                it[userId] = user.id
                it[top] = topBound
                it[bottom] = bottomBound
                it[results] = Json.serializer.encodeToString(graphOutputDto)
            }.named("save-graph-cache")
        }
    }

    private suspend fun findCache(redisEvent: RedisEvent, graphName: String, topBound: Int, bottomBound: Int): GraphOutputDto? {
        val userId = redisEvent.authorizedUser?.id ?: throw ForbiddenException()

        val cache = newAutoCommitTransaction(redisEvent) {
            this add GraphCacheModel.select(GraphCacheModel.results).where {
                (GraphCacheModel.top eq topBound) and (GraphCacheModel.name eq graphName) and (GraphCacheModel.bottom eq bottomBound) and (GraphCacheModel.userId eq userId)
            }
        }[GraphCacheModel]?.firstOrNull() ?: return null

        return Json.serializer.decodeFromString(cache[GraphCacheModel.results])
    }

    private fun buildBounds(graphConfigurationDto: GraphConfigurationDto): Pair<Int, Int> {
        val bottomBound = "${graphConfigurationDto.year}${graphConfigurationDto.month ?: "01"}${graphConfigurationDto.day ?: "01"}".toInt()
        val topBound = "${graphConfigurationDto.year}${graphConfigurationDto.month ?: "12"}${graphConfigurationDto.day ?: "31"}".toInt()
        return bottomBound to topBound
    }

    private fun Map<Int, List<Row>>.buildGraphData(mapper: (row: Row) -> Int): GraphOutputDto {
        var highest = -1.0
        var lowest = Double.MAX_VALUE
        var sum = 0.0

        val data = map { (date, rows) ->
            val points = rows.map(mapper)
            val point = points.average()
            highest = max(points.maxOrNull()?.toDouble() ?: -1.0, highest)
            lowest = min(points.minOrNull()?.toDouble() ?: Double.MAX_VALUE, lowest)
            highest = max(point, highest)
            lowest = min(point, lowest)
            sum += point
            date to point
        }.sortedBy { it.first }.toMap()

        if (sum == 0.0 || data.isEmpty()) {
            highest = 0.0
            lowest = 0.0
        }

        return GraphOutputDto(
            if (sum != 0.0 && data.isNotEmpty()) sum / data.size else 0.0,
            highest,
            lowest,
            data
        )
    }

    suspend fun buildHeartGraph(redisEvent: RedisEvent) {
        val config = redisEvent.parseData<GraphConfigurationDto>() ?: throw BadRequestException("Bad configuration provided")

        val (bottomBound, topBound) = buildBounds(config)

        val inCache = findCache(redisEvent, "heart", topBound, bottomBound)
        if (inCache != null) return redisEvent.switchOnApi(inCache)

        val heartData = newAutoCommitTransaction(redisEvent) {
            this add UserHeartLogModel.select().where { UserHeartLogModel.date.inRange(bottomBound, topBound) }
        }[UserHeartLogModel] ?: listOf()

        val graphOutputDto = if (config.day == null) {
            if (config.month == null) {
                heartData.groupBy {
                    val date = it[UserHeartLogModel.date]
                    val dayPart = date % 100
                    (date - dayPart) / 100
                }
            } else {
                heartData.groupBy { it[UserHeartLogModel.date] }
            }
        } else {
            heartData.groupBy { it[UserHeartLogModel.time] }
        }.buildGraphData { it[UserHeartLogModel.heartRate] }

//        cache(redisEvent, "heart", topBound, bottomBound, graphOutputDto)

        redisEvent.switchOnApi(graphOutputDto)
    }

    suspend fun buildSleepGraph(redisEvent: RedisEvent) {
        val config = redisEvent.parseData<GraphConfigurationDto>() ?: throw BadRequestException("Bad configuration provided")

        val (bottomBound, topBound) = buildBounds(config)

        val inCache = findCache(redisEvent, "sleep", topBound, bottomBound)
        if (inCache != null) return redisEvent.switchOnApi(inCache)

        val sleepData = newAutoCommitTransaction(redisEvent) {
            this add UserSleepModel.select().where { UserSleepModel.date.inRange(bottomBound, topBound) }
        }[UserSleepModel] ?: listOf()

        if (sleepData.isEmpty()) return redisEvent.switchOnApi(GraphOutputDto(0.0, 0.0, 0.0, emptyMap()))

        val graphOutputDto = if (config.day != null) {
            val item = sleepData.first()
            val highest = (item[UserSleepModel.sleepMinutes] + item[UserSleepModel.sleepHours] * 60).toDouble()
            GraphOutputDto(highest, highest, highest, mapOf(item[UserSleepModel.date] to highest))
        } else {
            if (config.month != null) {
                sleepData.groupBy { it[UserSleepModel.date] }
            } else {
                sleepData.groupBy {
                    val date = it[UserSleepModel.date]
                    val dayPart = date % 100
                    (date - dayPart) / 100
                }
            }.buildGraphData {
                it[UserSleepModel.sleepMinutes] + it[UserSleepModel.sleepHours] * 60
            }
        }

//        cache(redisEvent, "sleep", topBound, bottomBound, graphOutputDto)

        redisEvent.switchOnApi(graphOutputDto)
    }
}