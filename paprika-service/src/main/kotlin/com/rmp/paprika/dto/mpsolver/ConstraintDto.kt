package com.rmp.paprika.dto.mpsolver

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.column.Column

data class ConstraintDto (
    var name: String = "Constraint",
    var modelKey: Column<*> = DishModel.name,
    var bool: Boolean = false,
    var top: Double = 0.0,
    var bottom: Double = 0.0
)