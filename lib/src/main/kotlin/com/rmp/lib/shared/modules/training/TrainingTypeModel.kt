package com.rmp.lib.shared.modules.training

import com.rmp.lib.utils.korm.IdTable

object TrainingTypeModel: IdTable("training_type_model") {
    val name = text("type_name")
    val coefficient = double("type_coefficient")
}