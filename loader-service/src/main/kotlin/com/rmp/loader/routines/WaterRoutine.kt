package com.rmp.loader.routines

import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import com.rmp.loader.dto.water.AddWaterDto
import io.ktor.client.request.*

object WaterRoutine {
    val routine = Routine {
        startBot()


        addStep("users/stat/summary", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(date = TODAY))
            }
        }

        addDelay(100)

        addStep("users/log/water", ApiClient.Method.POST) {
            setBuilder { bot ->
                bot.state = randomInt(1003, 2359) - 2
                setBody(AddWaterDto(
                    date = TODAY,
                    time = bot.state.toString(),
                    volume = randomDouble(0.1, 10.1)
                ))
            }
        }

        addDelay(100)

        addStep("users/log/water", ApiClient.Method.POST) {
            setBuilder { bot ->
                bot.state = randomInt((bot.state as Int) + 1, 2359)
                setBody(AddWaterDto(
                    date = TODAY,
                    time = bot.state.toString(),
                    volume = randomDouble(0.1, 10.1)
                ))
            }
        }

        addDelay(100)

        addStep("users/stat/water", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY))
            }
        }

        addDelay(100)

        addStep("users/stat/water", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY - 1))
            }
        }

        addDelay(100)

        addStep("users/stat/water", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY - 2))
            }
        }

    }
}