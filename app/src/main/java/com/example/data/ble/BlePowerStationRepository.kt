package com.example.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.example.domain.models.BatteryData
import com.example.domain.models.BleDeviceInfo
import com.example.domain.models.BatteryStatus
import com.example.domain.models.ConnectionState
import com.example.domain.repository.PowerStationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BlePowerStationRepository(
    private val context: Context
) : PowerStationRepository {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryData = MutableStateFlow(BatteryData())
    override val batteryData: StateFlow<BatteryData> = _batteryData.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    override val scanResults: StateFlow<List<BleDeviceInfo>> = _scanResults.asStateFlow()

    private var currentGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        private const val TAG = "BleRepo"
        // Example UUIDs for the DIY ESP32 application
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E") // Nordic UART TX/RX
        val CHAR_TX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notifications
        val CHAR_RX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Writes
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: "Unknown Device"
                val address = device.address
                val rssi = result.rssi
                val isPowerStation = name.contains("PowerStation") || name.contains("ESP32") || name.contains("BMS")

                val newInfo = BleDeviceInfo(name, address, rssi, isPowerStation)

                _scanResults.update { currentList ->
                    val existingIdx = currentList.indexOfFirst { it.address == address }
                    if (existingIdx >= 0) {
                        val mutableList = currentList.toMutableList()
                        mutableList[existingIdx] = newInfo
                        mutableList
                    } else {
                        currentList + newInfo
                    }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                    currentGatt = gatt
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    currentGatt?.close()
                    currentGatt = null
                }
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                currentGatt?.close()
                currentGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                service?.let {
                    val notifyChar = it.getCharacteristic(CHAR_TX_UUID)
                    writeCharacteristic = it.getCharacteristic(CHAR_RX_UUID)
                    
                    if (notifyChar != null) {
                        gatt.setCharacteristicNotification(notifyChar, true)
                        val descriptor = notifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (descriptor != null) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java", ReplaceWith("parseAndEmitData(characteristic.value)"))
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == CHAR_TX_UUID) {
                parseAndEmitData(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == CHAR_TX_UUID) {
                parseAndEmitData(value)
            }
        }
    }

    private fun parseAndEmitData(bytes: ByteArray) {
        // Assume ESP32 sends tightly packed binary data (Float, Float, Float, Int, Float, Float, Int status)
        // For demonstration robustness in the absence of exact struct from user, 
        // we decode if length matches, OR fallback to string parsing if it's JSON/Comma separated.
        try {
            val str = String(bytes).trim()
            if (str.contains(",")) {
                // simple csv: V,A,Temp,SOC,Ah,Wh,Status
                val parts = str.split(",")
                if (parts.size >= 7) {
                    val statusInt = parts[6].toIntOrNull() ?: 0
                    val reserved = if (parts.size >= 8) parts[7].toIntOrNull() ?: 0 else 0
                    val parsedTemp = parts[2].toFloatOrNull() ?: 0f
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        _batteryData.update {
                            it.copy(
                                voltage = parts[0].toFloatOrNull() ?: 0f,
                                current = parts[1].toFloatOrNull() ?: 0f,
                                temperature = parsedTemp,
                                soc = parts[3].toIntOrNull() ?: 0,
                                remainingCapacityAh = parts[4].toFloatOrNull() ?: 0f,
                                remainingEnergyWh = parts[5].toFloatOrNull() ?: 0f,
                                status = BatteryStatus.values().getOrElse(statusInt) { BatteryStatus.IDLE },
                                reservedEnergyWh = if (parts.size >= 8) reserved else it.reservedEnergyWh
                            )
                        }
                    }
                }
            } else if (bytes.size >= 28) { // Binary struct fallback
                 val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                 val v = buffer.float
                 val i = buffer.float
                 val t = buffer.float
                 val soc = buffer.int
                 val ah = buffer.float
                 val wh = buffer.float
                 val statInt = buffer.int
                 CoroutineScope(Dispatchers.IO).launch {
                     _batteryData.update {
                        it.copy(
                            voltage = v, current = i, temperature = t, soc = soc,
                            remainingCapacityAh = ah, remainingEnergyWh = wh,
                            status = BatteryStatus.values().getOrElse(statInt) { BatteryStatus.IDLE }
                        )
                     }
                 }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing BLE payload", e)
        }
    }

    override fun startScanningAndConnect() {
        if (bluetoothAdapter?.isEnabled != true) return
        _connectionState.value = ConnectionState.SCANNING
        _scanResults.value = emptyList()

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }

    override fun connectToAddress(address: String) {
        if (bluetoothAdapter?.isEnabled != true) return
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        val device = bluetoothAdapter.getRemoteDevice(address)
        connectToDevice(device)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        currentGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    override fun disconnect() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        currentGatt?.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun setEnergyReservation(wh: Int) {
        writeCommand("RESRV:$wh")
        _batteryData.update { it.copy(reservedEnergyWh = wh) }
    }

    override fun unlockEnergyReservation() {
        writeCommand("UNLOCK")
        _batteryData.update { it.copy(reservedEnergyWh = 0) }
    }

    private fun writeCommand(cmd: String) {
        writeCharacteristic?.let { char ->
            val payload = cmd.toByteArray()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                currentGatt?.writeCharacteristic(char, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                char.value = payload
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                currentGatt?.writeCharacteristic(char)
            }
        }
    }
}
