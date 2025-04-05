package com.rmp.loader.routines

import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import io.ktor.client.request.*

object MenuHistoryRoutine {
    val routine = Routine {
        startBot()

        addDelay(100)

        addStep("users/stat/menu", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY))
            }
        }

        addDelay(100)

        addStep("users/stat/menu", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY - 1))
            }
        }

        addDelay(100)

        addStep("users/stat/menu", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY - 2))
            }
        }

        addDelay(100)

        addStep("users/stat/menu", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(TODAY - 3))
            }
        }
    }
}