package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ble.BlePowerStationRepository
import com.example.domain.engine.PredictionEngine
import com.example.domain.models.BatteryData
import com.example.domain.models.ConnectionState
import com.example.domain.repository.PowerStationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PowerStationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PowerStationRepository = BlePowerStationRepository(application)

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.DISCONNECTED)

    val scanResults = repository.scanResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteryData: StateFlow<BatteryData> = repository.batteryData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryData())

    val intelligenceSuggestions: StateFlow<List<String>> = repository.batteryData
        .map { PredictionEngine.getIntelligenceSuggestions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startScan() {
        if (connectionState.value == ConnectionState.DISCONNECTED) {
            repository.startScanningAndConnect()
        }
    }

    fun stopScan() {
        if (connectionState.value == ConnectionState.SCANNING) {
            repository.disconnect() // Calling disconnect will stop the scan inside the repo
        }
    }

    fun connectToAddress(address: String) {
        repository.connectToAddress(address)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun setReservedEnergy(wh: Int) {
        repository.setEnergyReservation(wh)
    }

    fun unlockReservedEnergy() {
        repository.unlockEnergyReservation()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
