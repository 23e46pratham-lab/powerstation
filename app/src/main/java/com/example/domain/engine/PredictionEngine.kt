package com.example.domain.engine

import com.example.domain.models.BatteryData
import com.example.domain.models.BatteryStatus

object PredictionEngine {

    // ─── EMA State ───────────────────────────────────────────────────────────
    private const val ALPHA = 0.1f          // smoothing factor (0.05 = very smooth, 0.2 = more reactive)
    private const val MIN_LOAD_WATTS = 5f   // below this = noise, don't calculate
    private const val INVERTER_EFFICIENCY = 0.88f  // for AC loads through inverter

    private var smoothedPower: Float = 0f
    private var lastStatus: BatteryStatus? = null

    private fun updateSmoothedPower(voltage: Float, current: Float, status: BatteryStatus) {
        // Reset EMA when switching from non-discharging to discharging
        // so stale values from charging don't bleed into runtime estimate
        if (lastStatus != BatteryStatus.DISCHARGING && status == BatteryStatus.DISCHARGING) {
            smoothedPower = 0f
        }
        lastStatus = status

        val instantPower = voltage * current
        smoothedPower = if (smoothedPower < 1f) {
            instantPower                                          // cold start: seed directly
        } else {
            (ALPHA * instantPower) + ((1f - ALPHA) * smoothedPower)  // EMA
        }
    }

    // ─── Current Load Runtime ─────────────────────────────────────────────────
    fun calculateCurrentLoadRuntime(data: BatteryData): String {
        if (data.status != BatteryStatus.DISCHARGING) {
            smoothedPower = 0f   // reset when not discharging
            return "No Active Load"
        }
        if (data.remainingEnergyWh <= 0f) return "--"

        updateSmoothedPower(data.voltage, data.current, data.status)

        if (smoothedPower < MIN_LOAD_WATTS) return "--"

        val hoursRemaining = (data.remainingEnergyWh * INVERTER_EFFICIENCY) / smoothedPower
        return formatTime(hoursRemaining)
    }

    // ─── Specific Device Runtime ──────────────────────────────────────────────
    fun calculateDeviceRuntime(data: BatteryData, devicePowerWatts: Float): String {
        if (data.remainingEnergyWh <= 0f || data.soc == 0) return "--"
        if (devicePowerWatts <= 0f) return "--"
        // Device runtime uses efficiency too since output is AC through inverter
        val hoursRemaining = (data.remainingEnergyWh * INVERTER_EFFICIENCY) / devicePowerWatts
        return formatTime(hoursRemaining)
    }

    // ─── Device Charging Count ────────────────────────────────────────────────
    fun calculateDeviceCharges(data: BatteryData, deviceCapacityWh: Float): String {
        if (data.remainingEnergyWh <= 0f) return "0 Times"
        if (deviceCapacityWh <= 0f) return "--"
        val charges = data.remainingEnergyWh / deviceCapacityWh
        return String.format("%.1f Times", charges)
    }

    // ─── Format ───────────────────────────────────────────────────────────────
    private fun formatTime(hoursDecimal: Float): String {
        if (hoursDecimal <= 0f) return "--"
        val totalMinutes = (hoursDecimal * 60).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "$hours hr $minutes min" else "$minutes min"
    }
}
