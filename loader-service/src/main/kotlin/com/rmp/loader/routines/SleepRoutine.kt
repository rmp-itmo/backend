package com.rmp.loader.routines

import com.rmp.loader.CURRENT_MONTH_GRAPH
import com.rmp.loader.CURRENT_YEAR_GRAPH
import com.rmp.loader.TODAY
import com.rmp.loader.TODAY_GRAPH
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import com.rmp.loader.dto.sleep.AddSleepDto
import io.ktor.client.request.*

object SleepRoutine {
    val routine = Routine {
        startBot()

        addDelay(100)

        addStep("users/stat/summary", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(date = TODAY))
            }
        }

        addDelay(100)

        addStep("sleep", ApiClient.Method.POST) {
            setBuilder {
                setBody(
                    AddSleepDto(
                        date = TODAY,
                        hours = randomInt(1, 10),
                        minutes = randomInt(1, 60),
                        quality = randomInt(1, 3)
                    )
                )
            }
        }

        addDelay(100)

        addStep("stat/graph/sleep", ApiClient.Method.POST) {
            setBuilder {
                setBody(TODAY_GRAPH)
            }
        }

        addDelay(100)

        addStep("stat/graph/sleep", ApiClient.Method.POST) {
            setBuilder {
                setBody(CURRENT_MONTH_GRAPH)
            }
        }

        addDelay(100)

        addStep("stat/graph/sleep", ApiClient.Method.POST) {
            setBuilder {
                setBody(CURRENT_YEAR_GRAPH)
            }
        }

    }
}