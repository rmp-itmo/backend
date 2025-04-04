package com.rmp.loader.routines

import com.rmp.loader.core.Routine
import com.rmp.loader.dto.LoginDto

object MenuRoutine {
    val routine = Routine {
        authorize(LoginDto("login@test.test", "password"))
    }
}