package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.NavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val permissionsGranted = remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions: Map<String, Boolean> ->
                    permissionsGranted.value = permissions.values.all { it }
                }

                LaunchedEffect(Unit) {
                    val requiredPermissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                        requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        requiredPermissions.add(Manifest.permission.BLUETOOTH)
                        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
                    }

                    val missingPermissions = requiredPermissions.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missingPermissions.isEmpty()) {
                        permissionsGranted.value = true
                    } else {
                        permissionLauncher.launch(missingPermissions.toTypedArray())
                    }
                }

                if (permissionsGranted.value) {
                    NavGraph()
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Bluetooth & Location permissions are required to connect to the Power Station.", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}
