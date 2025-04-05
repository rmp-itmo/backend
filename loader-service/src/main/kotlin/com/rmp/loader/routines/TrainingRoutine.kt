package com.rmp.loader.routines

import com.rmp.loader.CURRENT_MONTH
import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import com.rmp.loader.dto.trainings.TrainingsDto
import com.rmp.loader.dto.trainings.TrainingsUploadDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object TrainingRoutine {
    val routine = Routine {
        extend(MainPageRoutine.routine)

        addDelay(100)

        addStep("users/trainings/month", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(date = CURRENT_MONTH))
            }

            setProcessor { response ->
                state = response.body<TrainingsDto>()
            }
        }

        addDelay(100)

        addStep("users/trainings/log", ApiClient.Method.POST) {
            setBuilder {
                setBody(
                    TrainingsUploadDto(
                        date = TODAY,
                        start = randomInt(10, 15) * 100 + randomInt(10, 59),
                        end = randomInt(16, 19) * 100 + randomInt(10, 59),
                        type = 1,
                        intensity = 1
                    )
                )
            }
        }
    }
}