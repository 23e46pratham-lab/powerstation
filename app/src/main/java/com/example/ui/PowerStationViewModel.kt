package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ble.BlePowerStationRepository
import com.example.domain.engine.PredictionEngine
import com.example.domain.models.BatteryData
import com.example.domain.models.ConnectionState
import com.example.domain.repository.PowerStationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class DataPoint(val timestamp: Long, val soc: Int, val power: Float)

class PowerStationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PowerStationRepository = BlePowerStationRepository(application)

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.DISCONNECTED)

    val scanResults = repository.scanResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteryData: StateFlow<BatteryData> = repository.batteryData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryData())

    private val _history = MutableStateFlow<List<DataPoint>>(emptyList())
    val history: StateFlow<List<DataPoint>> = _history
    
    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds
    
    init {
        viewModelScope.launch {
            batteryData.collect { data ->
                if (connectionState.value == ConnectionState.CONNECTED) {
                    val currentList = _history.value.toMutableList()
                    currentList.add(DataPoint(System.currentTimeMillis(), data.soc, data.powerWatts))
                    // Keep last 100 points
                    if (currentList.size > 100) {
                        currentList.removeAt(0)
                    }
                    _history.value = currentList
                }
            }
        }
        
        viewModelScope.launch {
            while(true) {
                if (connectionState.value == ConnectionState.CONNECTED) {
                    _uptimeSeconds.value += 1
                } else {
                    _uptimeSeconds.value = 0
                }
                delay(1000)
            }
        }
    }

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
