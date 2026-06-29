package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import com.example.domain.engine.PredictionEngine
import com.example.domain.models.ConnectionState
import com.example.ui.PowerStationViewModel
import com.example.ui.components.BatteryGauge
import com.example.ui.theme.*

@Composable
fun DashboardScreen(viewModel: PowerStationViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val batteryData by viewModel.batteryData.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()
    val scrollState = rememberScrollState()

    var showConnectionDialog by remember { mutableStateOf(false) }

    // Automated scanning, auto-connecting, and timeout logic inside the dashboard
    LaunchedEffect(showConnectionDialog, connectionState) {
        if (showConnectionDialog) {
            if (connectionState == ConnectionState.DISCONNECTED) {
                viewModel.startScan()
            }
            if (connectionState == ConnectionState.CONNECTED) {
                showConnectionDialog = false
            }
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.SCANNING) {
            kotlinx.coroutines.delay(10000L)
            if (viewModel.connectionState.value == ConnectionState.SCANNING) {
                viewModel.stopScan()
            }
        }
    }

    LaunchedEffect(scanResults, connectionState) {
        if (showConnectionDialog && connectionState == ConnectionState.SCANNING) {
            val matching = scanResults.filter { it.isPowerStation }
            if (matching.size == 1) {
                viewModel.connectToAddress(matching.first().address)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        TopBar(connectionState) {
            if (connectionState == ConnectionState.CONNECTED) {
                viewModel.disconnect()
            } else {
                showConnectionDialog = true
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState != ConnectionState.CONNECTED) {
            DisconnectedHeaderCard(connectionState) {
                showConnectionDialog = true
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        BatteryGauge(soc = batteryData.soc)

        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(status = batteryData.status.name, reserved = batteryData.reservedEnergyWh, powerWatts = batteryData.powerWatts)

        Spacer(modifier = Modifier.height(24.dp))

        MetricsGrid(
            voltage = batteryData.voltage,
            current = batteryData.current,
            energyWh = batteryData.remainingEnergyWh,
            capacityAh = batteryData.remainingCapacityAh,
            temp = batteryData.temperature
        )

        Spacer(modifier = Modifier.height(24.dp))

        PredictionsSection(
            currentLoadRuntime = PredictionEngine.calculateCurrentLoadRuntime(batteryData),
            laptopRuntime = PredictionEngine.calculateDeviceRuntime(batteryData, 60f),
            fanRuntime = PredictionEngine.calculateDeviceRuntime(batteryData, 30f),
            phoneCharges = PredictionEngine.calculateDeviceCharges(batteryData, 15f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val suggestions by viewModel.intelligenceSuggestions.collectAsState()
        if (suggestions.isNotEmpty()) {
            IntelligenceSection(suggestions)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            viewModel = viewModel,
            connectionState = connectionState,
            scanResults = scanResults,
            onDismissRequest = { showConnectionDialog = false }
        )
    }
}

@Composable
fun IntelligenceSection(suggestions: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Battery Intelligence",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                suggestions.forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PowerYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = suggestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(connectionState: ConnectionState, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (connectionState == ConnectionState.CONNECTED) "SYSTEM ONLINE" else "SYSTEM OFFLINE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = if (connectionState == ConnectionState.CONNECTED) Emerald500 else PowerRed
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DIY PowerStation v1",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        OutlinedButton(
            onClick = onToggle,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Zinc900.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            border = BorderStroke(1.dp, if (connectionState == ConnectionState.CONNECTED) Emerald500.copy(alpha=0.3f) else if (connectionState == ConnectionState.DISCONNECTED) Zinc800 else TechBlue.copy(alpha=0.3f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> Emerald500
                            ConnectionState.CONNECTING -> PowerYellow
                            ConnectionState.SCANNING -> TechBlue
                            ConnectionState.DISCONNECTED -> Zinc500
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "Disconnect"
                    ConnectionState.CONNECTING -> "Connecting..."
                    ConnectionState.SCANNING -> "Scanning..."
                    ConnectionState.DISCONNECTED -> "Connect"
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun DisconnectedHeaderCard(connectionState: ConnectionState, onConnectClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = PowerRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PowerRed.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Offline",
                    tint = PowerRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "PowerStation Disconnected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Telemetries and prediction systems are offline. Connect to your DIY PowerStation via Bluetooth.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = PowerRed),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (connectionState == ConnectionState.SCANNING) "Scanning..." else "Connect Device",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ConnectionDialog(
    viewModel: PowerStationViewModel,
    connectionState: ConnectionState,
    scanResults: List<com.example.domain.models.BleDeviceInfo>,
    onDismissRequest: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            viewModel.stopScan()
            onDismissRequest()
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Zinc800)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect Device",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = {
                        viewModel.stopScan()
                        onDismissRequest()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Zinc400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (connectionState == ConnectionState.SCANNING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = Emerald500,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Scanning for PowerStations...",
                            color = Emerald400,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (connectionState == ConnectionState.CONNECTING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = Emerald500,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Connecting...",
                            color = Emerald400,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.startScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan for Devices", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (scanResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (connectionState == ConnectionState.SCANNING) "Searching..." else "No devices found yet.",
                            color = Zinc500,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortedResults = scanResults.sortedByDescending { it.isPowerStation }
                        items(sortedResults) { device ->
                            DialogDeviceRow(device = device, onClick = {
                                if (connectionState != ConnectionState.CONNECTING) {
                                    viewModel.connectToAddress(device.address)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogDeviceRow(device: com.example.domain.models.BleDeviceInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Zinc900),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Zinc400
                )
            }
            if (device.isPowerStation) {
                Badge(
                    containerColor = Emerald500.copy(alpha = 0.2f),
                    contentColor = Emerald400,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("PowerStation", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = Zinc400
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SignalWifi4Bar,
                    contentDescription = "Signal",
                    tint = Zinc400,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun StatusCard(status: String, reserved: Int, powerWatts: Float) {
    val isCharging = status == com.example.domain.models.BatteryStatus.CHARGING.name
    val color = if (reserved > 0) PowerRed else if (isCharging) Emerald400 else Zinc400
    val bgColor = if (reserved > 0) PowerRed.copy(alpha=0.1f) else Emerald500.copy(alpha=0.1f)
    val borderColor = if (reserved > 0) PowerRed.copy(alpha=0.3f) else Emerald500.copy(alpha=0.3f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(bgColor, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, borderColor, androidx.compose.foundation.shape.CircleShape)
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            val text = if (reserved > 0) "Reserved ${reserved}Wh" else "${status.replace("_", " ")}"
            Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "${String.format("%.1f", powerWatts)} W", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Zinc300)
    }
}

@Composable
fun MetricsGrid(voltage: Float, current: Float, energyWh: Float, capacityAh: Float, temp: Float) {
    val formattedTemp = formatTemp(temp)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBox("Voltage", String.format("%.2f", voltage), "V", Modifier.weight(1f))
            MetricBox("Current", String.format("%.2f", current), "A", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBox("Energy", String.format("%.1f", energyWh), "Wh", Modifier.weight(1f))
            MetricBox("Capacity", String.format("%.2f", capacityAh), "Ah", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBox("Temperature", formattedTemp, "°C", Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Zinc500
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Zinc400,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PredictionsSection(currentLoadRuntime: String, laptopRuntime: String, fanRuntime: String, phoneCharges: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUNTIME PREDICTIONS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Zinc400
            )
            Text(
                text = "EKF Optimized",
                style = MaterialTheme.typography.labelSmall,
                color = Emerald500
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PredictionBox("Total Load", currentLoadRuntime, true)
            }
            item {
                PredictionBox("Laptop Charge", laptopRuntime, false)
            }
            item {
                PredictionBox("Fan Runtime", fanRuntime, false)
            }
        }
    }
}

@Composable
fun PredictionBox(title: String, value: String, isPrimary: Boolean) {
    val bgColor = if (isPrimary) Emerald600.copy(alpha = 0.1f) else DarkSurface
    val borderColor = if (isPrimary) Emerald500.copy(alpha = 0.2f) else Zinc800
    val titleColor = if (isPrimary) Emerald400 else Zinc400

    Box(
        modifier = Modifier
            .width(130.dp)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatTemp(temp: Float): String =
    if (temp <= -999f || temp.isNaN()) "--" else String.format("%.1f", temp)
