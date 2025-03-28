package com.rmp.lib.shared.modules.training

import com.rmp.lib.utils.korm.IdTable

object TrainingIntensityModel: IdTable("training_intensity_model") {
    val name = text("intensity_name")
    val coefficient = double("intensity_coefficient")
}