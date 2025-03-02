package com.rmp.lib.exceptions

import kotlinx.serialization.Serializable

@Serializable
class ForbiddenException: BaseException(403, "Forbidden")