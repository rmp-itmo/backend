package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.hello.LoginDto

object MenuRoutine {
    val routine = Routine {
        authorize(LoginDto("login@test.test", "password"))

        addDelay(100) // Delay between 50 and 150

        addStep("paprika/calculate", ApiClient.Method.POST) {
            setBuilder {
//                val
            }
        }
    }
}