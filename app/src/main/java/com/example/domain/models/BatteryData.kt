package com.example.domain.models

data class BatteryData(
    val voltage: Float = 0f,
    val current: Float = 0f,
    val temperature: Float = 0f,
    val soc: Int = 0,
    val remainingCapacityAh: Float = 0f,
    val remainingEnergyWh: Float = 0f,
    val status: BatteryStatus = BatteryStatus.IDLE,
    val reservedEnergyWh: Int = 0
) {
    val powerWatts: Float
        get() = voltage * current
}

enum class BatteryStatus {
    IDLE,
    CHARGING,
    DISCHARGING,
    FULL,
    LOW_BATTERY,
    CRITICAL_BATTERY
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}
