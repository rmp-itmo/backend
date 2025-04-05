package com.rmp.loader.routines

import com.rmp.loader.CURRENT_MONTH_GRAPH
import com.rmp.loader.CURRENT_YEAR_GRAPH
import com.rmp.loader.TODAY
import com.rmp.loader.TODAY_GRAPH
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.heart.AddHeartLogDto
import io.ktor.client.request.*

object HeartRoutine {
    val routine = Routine {
        startBot()

        addStep("users/log/heart", ApiClient.Method.POST) {
            setBuilder { bot ->
                bot.state = randomInt(1003, 2359) - 2
                setBody(AddHeartLogDto(
                    date = TODAY,
                    heartRate = randomInt(60, 200),
                    time = bot.state as Int,
                ))
            }
        }

        addDelay(100)

        addStep("stat/graph/heart", ApiClient.Method.POST) {
            setBuilder {
                setBody(TODAY_GRAPH)
            }
        }

        addDelay(100)


        addStep("users/log/heart", ApiClient.Method.POST) {
            setBuilder { bot ->
                bot.state = randomInt((bot.state as Int) + 1, 2359)
                setBody(AddHeartLogDto(
                    date = TODAY,
                    heartRate = randomInt(60, 200),
                    time = bot.state as Int,
                ))
            }
        }

        addDelay(100)

        addStep("stat/graph/heart", ApiClient.Method.POST) {
            setBuilder {
                setBody(TODAY_GRAPH)
            }
        }

        addDelay(100)

        addStep("stat/graph/heart", ApiClient.Method.POST) {
            setBuilder {
                setBody(TODAY_GRAPH)
            }
        }

        addDelay(100)

        addStep("stat/graph/heart", ApiClient.Method.POST) {
            setBuilder {
                setBody(CURRENT_MONTH_GRAPH)
            }
        }

        addDelay(100)

        addStep("stat/graph/heart", ApiClient.Method.POST) {
            setBuilder {
                setBody(CURRENT_YEAR_GRAPH)
            }
        }
    }
}