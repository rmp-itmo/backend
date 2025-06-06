package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.training.TrainingIntensityModel
import com.rmp.lib.shared.modules.training.TrainingTypeModel
import com.rmp.lib.shared.modules.training.UserTrainingLogModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.korm.references.JoinType
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.user.dto.trainings.intensity.TrainingIntensityListOutputDto
import com.rmp.user.dto.trainings.intensity.TrainingIntensityOutputDto
import com.rmp.user.dto.trainings.log.get.TrainingGetLogInputDto
import com.rmp.user.dto.trainings.log.set.TrainingLogInputDto
import com.rmp.user.dto.trainings.log.set.TrainingLogOutputDto
import com.rmp.user.dto.trainings.types.TrainingsTypeListOutputDto
import com.rmp.user.dto.trainings.types.TrainingsTypeOutputDto
import org.kodein.di.DI
import com.rmp.lib.utils.korm.column.inRange
import com.rmp.lib.utils.korm.query.builders.OrderBy
import com.rmp.user.dto.trainings.log.get.TrainingOutputDto
import com.rmp.user.dto.trainings.log.get.TrainingsListOutputDto

class TrainingService(di: DI): FsmService(di) {

    suspend fun getTypes(redisEvent: RedisEvent) {
        val types = newAutoCommitTransaction(redisEvent) {
            this add TrainingTypeModel.select()
        }[TrainingTypeModel] ?: listOf()

        redisEvent.switchOnApi(
            TrainingsTypeListOutputDto(
                types.map {
                    TrainingsTypeOutputDto(
                        it[TrainingTypeModel.id],
                        it[TrainingTypeModel.name]
                    )
                }
            )
        )
    }

    suspend fun getIntensity(redisEvent: RedisEvent) {
        val intensity = newAutoCommitTransaction(redisEvent) {
            this add TrainingIntensityModel
                .select(TrainingIntensityModel.id, TrainingIntensityModel.name)
        }[TrainingIntensityModel] ?: throw InternalServerException("Failed select")

        redisEvent.switchOnApi(
            TrainingIntensityListOutputDto(
                intensity.map {
                    TrainingIntensityOutputDto(
                        it[TrainingIntensityModel.id],
                        it[TrainingIntensityModel.name]
                    )
                }
            )
        )
    }

    suspend fun logTraining(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<TrainingLogInputDto>() ?: throw BadRequestException("Bad data provided")

        if (data.start > data.end) throw BadRequestException("Start time cannot be greater then end")

        val select = newAutoCommitTransaction(redisEvent) {
            this add TrainingTypeModel
                .select(TrainingTypeModel.coefficient)
                .where { TrainingTypeModel.id eq data.type }

            this add TrainingIntensityModel
                .select(TrainingIntensityModel.coefficient)
                .where { TrainingIntensityModel.id eq data.intensity }
        }

        val trainingType = select[TrainingTypeModel]?.firstOrNull()
            ?: throw BadRequestException("Training type doesn't exist")

        val trainingIntensity = select[TrainingIntensityModel]?.firstOrNull()
            ?: throw BadRequestException("Training intensity doesn't exist")

        val (startMinutes, endMinutes) = (data.start % 100) to (data.end % 100)
        val (startHours, endHours) = (data.start / 100) to (data.end / 100)

        val totalTime = (endHours - startHours) * 60 + (endMinutes - startMinutes)

        val caloriesBurned = totalTime * trainingType[TrainingTypeModel.coefficient] *
                trainingIntensity[TrainingIntensityModel.coefficient]

        val insert = newAutoCommitTransaction(redisEvent) {
            this add UserTrainingLogModel
                .insert {
                    it[user] = authorizedUser.id
                    it[startAt] = data.start
                    it[endAt] = data.end
                    it[date] = data.date
                    it[type] = data.type
                    it[intensity] = data.intensity
                    it[calories] = caloriesBurned
                }.named("insert-user-training-log")
        }["insert-user-training-log"]?.firstOrNull() ?: throw InternalServerException("Failed insert")

        redisEvent.switchOnApi(TrainingLogOutputDto(insert[UserTrainingLogModel.id]))
    }

    suspend fun getTrainings(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<TrainingGetLogInputDto>() ?: throw BadRequestException("Bad data provided")

        val start = data.date * 100
        val end = if (data.date % 100 == 12) {
            (data.date + 89) * 100
        } else {
            (data.date + 1) * 100
        }

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserTrainingLogModel.select(
                UserTrainingLogModel.id,
                UserTrainingLogModel.startAt,
                UserTrainingLogModel.endAt,
                UserTrainingLogModel.calories,
                UserTrainingLogModel.date,
                TrainingTypeModel.name,
                TrainingIntensityModel.name
            )
                .join(TrainingTypeModel, JoinType.INNER, TrainingTypeModel.id eq UserTrainingLogModel.type)
                .join(TrainingIntensityModel, JoinType.INNER, TrainingIntensityModel.id eq UserTrainingLogModel.intensity)
                .orderBy(UserTrainingLogModel.date, OrderBy.DESC)
                .where {
                        (UserTrainingLogModel.user eq authorizedUser.id) and
                        (UserTrainingLogModel.date.inRange(start, end))
                }

            this add UserModel.select(UserModel.stepsTarget, UserModel.stepsCount).where { UserModel.id eq authorizedUser.id }
        }

        val trainings = select[UserTrainingLogModel] ?: listOf()
        val user = select[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to select steps target")

        fun Int.fixTime(): String = if (this < 10) "0$this" else "$this"

        val result = trainings.groupBy({ it[UserTrainingLogModel.date] }) {
            val (startMinutes, endMinutes) = (it[UserTrainingLogModel.startAt] % 100).fixTime() to (it[UserTrainingLogModel.endAt] % 100).fixTime()
            val (startHours, endHours) = (it[UserTrainingLogModel.startAt] / 100).fixTime() to (it[UserTrainingLogModel.endAt] / 100).fixTime()

            TrainingOutputDto(
                it[UserTrainingLogModel.id],
                "$startHours:$startMinutes",
                "$endHours:$endMinutes",
                it[UserTrainingLogModel.calories],
                it[TrainingTypeModel.name],
                it[TrainingIntensityModel.name]
            )
        }

        redisEvent.switchOnApi(
            TrainingsListOutputDto(
                stepsTarget = user[UserModel.stepsTarget],
                stepsCurrent = user[UserModel.stepsCount],
                trainings = result
            )
        )
    }
}