package com.example.domain.engine

import com.example.domain.models.BatteryData

object PredictionEngine {
    // Current load runtime
    fun calculateCurrentLoadRuntime(data: BatteryData): String {
        // If not discharging, there is no active load to calculate for
        if (data.status != com.example.domain.models.BatteryStatus.DISCHARGING) return "No Active Load"
        val power = data.voltage * data.current   // both positive, this gives real watts
        if (power < 1f) return "--"
        if (data.remainingEnergyWh <= 0f) return "--"
        val hoursRemaining = data.remainingEnergyWh / power
        return formatTime(hoursRemaining)
    }

    // Specific device runtimes
    fun calculateDeviceRuntime(data: BatteryData, devicePowerWatts: Float): String {
        if (data.remainingEnergyWh <= 0f || data.soc == 0) return "--"
        val hoursRemaining = data.remainingEnergyWh / devicePowerWatts
        return formatTime(hoursRemaining)
    }

    // Device charing (Times)
    fun calculateDeviceCharges(data: BatteryData, deviceCapacityWh: Float): String {
        if (data.remainingEnergyWh <= 0f) return "0 Times"
        val charges = data.remainingEnergyWh / deviceCapacityWh
        return String.format("%.1f Times", charges)
    }

    private fun formatTime(hoursDecimal: Float): String {
        val totalMinutes = (hoursDecimal * 60).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        if (hours > 0) {
            return "$hours Hours $minutes Minutes"
        }
        return "$minutes Minutes"
    }

    fun getIntelligenceSuggestions(data: BatteryData): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (data.voltage < 1f) return emptyList() // no real data yet

        if (data.temperature > 40f) {
            suggestions.add("Battery temperature is high. Consider reducing load or improving ventilation.")
        } else if (data.temperature in 15f..35f) {
            suggestions.add("Battery temperature is optimal.")
        }

        if (data.soc > 80) {
            suggestions.add("Battery is healthy and sufficiently charged.")
        } else if (data.soc < 20) {
            suggestions.add("Battery should be recharged soon. Runtime is limited.")
        }

        if (data.status == com.example.domain.models.BatteryStatus.DISCHARGING && data.powerWatts > 200f) { // High discharge
            suggestions.add("High load detected. This may exhaust the battery quickly.")
        }

        suggestions.add("Can charge a typical smartphone (15Wh) ~${calculateDeviceCharges(data, 15f).replace(" Times", "")} times.")

        return suggestions
    }
}
