package com.example.domain.models

data class BleDeviceInfo(
    val name: String,
    val address: String,
    val rssi: Int,
    val isPowerStation: Boolean
)
