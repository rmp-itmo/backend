package com.rmp.lib.exceptions

class CantSolveException(data: String? = "Solution not found"): BaseException(400, "CantSolve", data)