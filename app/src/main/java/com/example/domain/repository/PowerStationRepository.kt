package com.example.domain.repository

import com.example.domain.models.BatteryData
import com.example.domain.models.BleDeviceInfo
import com.example.domain.models.ConnectionState
import kotlinx.coroutines.flow.Flow

interface PowerStationRepository {
    val connectionState: Flow<ConnectionState>
    val batteryData: Flow<BatteryData>
    val scanResults: Flow<List<BleDeviceInfo>>
    
    fun startScanningAndConnect()
    fun connectToAddress(address: String)
    fun disconnect()
    fun setEnergyReservation(wh: Int)
    fun unlockEnergyReservation()
}

