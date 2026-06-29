package com.example.ui.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.PowerStationViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.Indigo200
import com.example.ui.theme.Indigo300
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo900
import com.example.ui.theme.Blue900
import com.example.ui.theme.PowerRed
import com.example.ui.theme.TechBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(viewModel: PowerStationViewModel) {
    val batteryData by viewModel.batteryData.collectAsState()
    var inputWh by remember { mutableStateOf("") }
    
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            Indigo900.copy(alpha = 0.4f),
            Blue900.copy(alpha = 0.2f)
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Energy Reservation",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Reserve energy for critical devices. The power station will cut off normal power when remaining energy reaches this limit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient, RoundedCornerShape(24.dp))
                .border(1.dp, Indigo500.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (batteryData.reservedEnergyWh > 0) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Reserved",
                        tint = Indigo300,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${batteryData.reservedEnergyWh} Wh Reserved",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Indigo200
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Output will be disabled to protect this energy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Indigo300.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.unlockReservedEnergy() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock Reserved Energy", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                } else {
                    OutlinedTextField(
                        value = inputWh,
                        onValueChange = { inputWh = it },
                        label = { Text("Reserved Energy (Wh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Indigo400,
                            unfocusedIndicatorColor = Indigo500.copy(alpha = 0.5f),
                            focusedLabelColor = Indigo400,
                            unfocusedLabelColor = Indigo300,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Indigo200,
                            unfocusedTextColor = Indigo200
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickSelectButton("50 Wh", { inputWh = "50" }, Modifier.weight(1f))
                        QuickSelectButton("100 Wh", { inputWh = "100" }, Modifier.weight(1f))
                        QuickSelectButton("150 Wh", { inputWh = "150" }, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            val wh = inputWh.toIntOrNull()
                            if (wh != null) viewModel.setReservedEnergy(wh)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Activate Reservation", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSelectButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Indigo500.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo200)
    ) {
        Text(text)
    }
}
