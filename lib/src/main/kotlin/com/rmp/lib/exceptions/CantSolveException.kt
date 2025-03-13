package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

@Serializable
class CantSolveException(
    override val message: String = "Solution not found"
): BaseException(400, "CantSolve", message)