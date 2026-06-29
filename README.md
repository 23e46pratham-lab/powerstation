<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/MCU-ESP32-E7352C?style=for-the-badge&logo=espressif&logoColor=white"/>
<img src="https://img.shields.io/badge/Protocol-BLE%20NUS-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white"/>
<img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge"/>

<br/>
<br/>

# ⚡ PowerStation Monitor

**Real-time BLE monitoring and control for 4S LiFePO4 battery packs**

Remote telemetry · SOC estimation · Energy reservation · Runtime prediction

<br/>

[Features](#-features) · [Architecture](#-architecture) · [Getting Started](#-getting-started) · [BLE Protocol](#-ble-protocol) · [Firmware](#-firmware-esp32) · [Screenshots](#-screenshots) · [Contributing](#-contributing)

</div>

---

## Overview

PowerStation Monitor is an open-source Android application that pairs with a custom ESP32-based battery management system to provide real-time remote monitoring of a 4-cell LiFePO₄ (LiFePO4) battery pack.

The ESP32 firmware runs a **Thevenin-model Extended Kalman Filter (EKF)** for accurate State of Charge (SOC) estimation, sampling voltage via an ADS1115 ADC, current via an ACS758 hall-effect sensor, and temperature via a DS18B20 sensor. All data is streamed to the Android app over **Bluetooth Low Energy (BLE)** using the Nordic UART Service (NUS) protocol.

The app provides live telemetry, runtime predictions, battery health metrics, and a unique **energy reservation** feature that sends a hardware-level cutoff command back to the ESP32 to protect a defined portion of battery capacity for critical use.

---

## ✨ Features

### Android App
- **Live Dashboard** — pack voltage, current draw, temperature, SOC gauge, power, and operating status in one glance
- **Runtime Predictions** — automatically calculates estimated runtime for the current load, a laptop (60 W), a fan (30 W), and smartphone charge cycles
- **Energy Reservation** — reserve a fixed amount of Wh via BLE command; the ESP32 enforces a hardware output cutoff when that threshold is reached
- **Battery Health** — temperature monitoring with cold/hot threshold indicators
- **AI Intelligence Panel** — context-aware suggestions based on live SOC, temperature, and load
- **BLE Auto-Connect** — scans and connects automatically to any device advertising as `PowerStation`, `ESP32`, or `BMS`
- **Connection State Indicator** — clear DISCONNECTED → SCANNING → CONNECTING → CONNECTED lifecycle

### ESP32 Firmware
- **Thevenin EKF** — R0 = 20 mΩ, R1 = 30 mΩ, C1 = 1140 F model with adaptive measurement noise weighting
- **Forced SOC thresholds** — hard-clips SOC to 96 / 98 / 100 % at defined pack voltages to prevent drift at high SOC
- **OCV rest correction** — after 60 s of rest with |I| < 100 mA, applies OCV-based SOC reset
- **Temperature compensation** — adjusts effective capacity and internal resistance at cold/hot extremes
- **NVS persistence** — SOC, EKF covariance, and ACS zero calibration survive power cycles
- **16×2 LCD** — pack voltage, current, SOC%, temperature, cold/hot flag, and operating status
- **128×64 OLED Robo Eyes** — animated eye display with 11 expression states driven by battery conditions

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Android App (Kotlin)                │
│                                                     │
│  ┌──────────────┐    ┌──────────────────────────┐   │
│  │  UI Screens  │◄───│  PowerStationViewModel   │   │
│  │  (Compose)   │    │  (AndroidViewModel)      │   │
│  └──────────────┘    └────────────┬─────────────┘   │
│                                   │ StateFlow        │
│                      ┌────────────▼─────────────┐   │
│                      │ BlePowerStationRepository │   │
│                      │  (PowerStationRepository) │   │
│                      └────────────┬─────────────┘   │
└───────────────────────────────────┼─────────────────┘
                                    │ BLE (NUS)
                    ┌───────────────▼───────────────┐
                    │         ESP32 Firmware        │
                    │                               │
                    │  loop() ──► EKF ──► BLE TX   │
                    │  BLE RX ──► Command Parser   │
                    │         ──► Relay Cutoff      │
                    └───────────────────────────────┘
```

### Module Structure

```
app/src/main/java/com/example/
├── data/
│   └── ble/
│       └── BlePowerStationRepository.kt   # BLE scan, GATT, parse, write
├── domain/
│   ├── engine/
│   │   └── PredictionEngine.kt            # Runtime & charge prediction logic
│   ├── models/
│   │   └── BatteryData.kt                 # Data class + BatteryStatus + ConnectionState enums
│   └── repository/
│       └── PowerStationRepository.kt      # Interface (StateFlow contract)
└── ui/
    ├── analytics/AnalyticsScreen.kt       # Historical charts (placeholder)
    ├── components/Gauge.kt                # SOC arc gauge composable
    ├── dashboard/DashboardScreen.kt       # Main live-data screen
    ├── health/HealthScreen.kt             # Battery health & temperature
    ├── reservation/ReservationScreen.kt   # Energy reservation control
    ├── settings/SettingsScreen.kt         # App configuration
    ├── theme/                             # Material 3 dark theme + custom colours
    ├── NavGraph.kt                        # Bottom-nav navigation
    └── PowerStationViewModel.kt           # Shared ViewModel
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Android Studio | Ladybug (2024.2+) |
| Android SDK | API 24 (Android 7.0) |
| Target SDK | API 36 |
| Kotlin | 2.2.10 |
| Gradle | 9.1.1 |
| Physical Android device | Required (BLE doesn't work on emulator) |

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/powerstation-monitor.git
cd powerstation-monitor
```

### 2. Configure API Keys

Copy the example environment file and add your Gemini API key (used for the AI intelligence panel):

```bash
cp .env.example .env
```

Edit `.env`:
```
GEMINI_API_KEY=your_key_here
```

### 3. Fix Signing Config

Open `app/build.gradle.kts` and remove or replace this line in the `debug` build type if you don't have the debug keystore:

```kotlin
// Remove this line:
signingConfig = signingConfigs.getByName("debugConfig")
```

### 4. Build & Run

Open the project in Android Studio, select your physical Android device, and press **Run** (`⌘R` / `Shift+F10`).

### Android Permissions

The app declares and requests the following permissions at runtime:

```xml
BLUETOOTH
BLUETOOTH_ADMIN
BLUETOOTH_SCAN          <!-- Android 12+ -->
BLUETOOTH_CONNECT       <!-- Android 12+ -->
ACCESS_FINE_LOCATION    <!-- Required for BLE scan on Android < 12 -->
ACCESS_COARSE_LOCATION
```

---

## 📡 BLE Protocol

The ESP32 and Android app communicate exclusively over **Bluetooth Low Energy** using the **Nordic UART Service (NUS)**.

### Service & Characteristic UUIDs

| Role | UUID | Properties |
|------|------|------------|
| NUS Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` | — |
| TX (ESP32 → App) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | NOTIFY |
| RX (App → ESP32) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | WRITE |
| CCCD Descriptor | `00002902-0000-1000-8000-00805F9B34FB` | Enable notify |

The ESP32 must advertise a device name containing one of: `PowerStation`, `ESP32`, or `BMS`.

---

### Telemetry Packet (ESP32 → App)

Sent every **500 ms** as a newline-terminated ASCII CSV string:

```
<V>,<A>,<T>,<SOC>,<Ah>,<Wh>,<STATUS>\n
```

**Example:**
```
13.42,2.35,28.6,87,13.05,166.8,2\n
```

| Position | Field | Unit | Arduino Variable | Format |
|----------|-------|------|-----------------|--------|
| 0 | Voltage | V | `batteryVoltage` | `%.2f` |
| 1 | Current | A | `I_display` (always ≥ 0) | `%.2f` |
| 2 | Temperature | °C | `temperature` | `%.1f` (send `-999` if sensor disconnected) |
| 3 | SOC | % | `(int)soc` | integer 0–100 |
| 4 | Remaining Capacity | Ah | `(soc/100) × BATTERY_CAPACITY_AH_eff` | `%.2f` |
| 5 | Remaining Energy | Wh | `remainingCapacityAh × batteryVoltage` | `%.1f` |
| 6 | Status | enum | see table below | integer 0–5 |

#### Status Code Mapping

| Integer | `BatteryStatus` enum | Condition (Arduino) |
|---------|---------------------|-------------------|
| `0` | `IDLE` | `\|currentForUse\| < 0.05 A` |
| `1` | `CHARGING` | `currentForUse < -0.05 A` |
| `2` | `DISCHARGING` | `currentForUse > 0.05 A` |
| `3` | `FULL` | `soc ≥ 99 % && forced == true` |
| `4` | `LOW_BATTERY` | `soc ≤ 20 %` |
| `5` | `CRITICAL_BATTERY` | `soc ≤ 5 %` |

> **Priority order:** `CRITICAL_BATTERY` > `LOW_BATTERY` > `FULL` > `CHARGING` > `DISCHARGING` > `IDLE`

---

### Commands (App → ESP32)

Commands are written as plain UTF-8 strings to the RX characteristic.

| Command | Trigger | ESP32 Action |
|---------|---------|-------------|
| `RESRV:<Wh>` | User activates energy reservation | Store `reservedEnergyWh`; open relay when `remainingWh ≤ reservedWh` |
| `UNLOCK` | User unlocks reserved energy | Set `reservedEnergyWh = 0`; close relay / re-enable output |

**Example:**
```
RESRV:100    → reserve 100 Wh
UNLOCK       → clear reservation
```

---

## 🔧 Firmware (ESP32)

> The Arduino firmware source is maintained in a separate directory. The BLE module must be added to the existing `.ino` file — see **[`docs/BLE_Protocol.docx`](docs/BLE_Protocol.docx)** for the complete integration guide including copy-paste-ready code blocks.

### Hardware

| Component | Part | Pin |
|-----------|------|-----|
| MCU | ESP32 (any 38-pin dev board) | — |
| ADC | ADS1115 (I²C) | SDA=21, SCL=22 |
| Current sensor | ACS758-050B | ADS1115 CH0 |
| Voltage divider | 5:1 ratio | ADS1115 CH1 |
| Temperature | DS18B20 (OneWire) | GPIO 4 |
| LCD | 16×2 I²C (0x27) | SDA=21, SCL=22 |
| OLED | SSD1306 / SH1106 128×64 I²C | SDA=21, SCL=22 |
| Output relay | Any 5 V relay module | GPIO 5 (configurable) |

### Required Arduino Libraries

```
Adafruit ADS1X15
Adafruit GFX
Adafruit SH110X
DallasTemperature
LiquidCrystal_I2C
OneWire
ESP32 BLE Arduino (built-in with ESP32 Arduino core)
```

### Key Firmware Constants

```cpp
const float BATTERY_CAPACITY_AH = 15.0f;  // pack capacity
const float VOLTAGE_RATIO       = 5.0f;   // divider ratio: Vbat = Vsense × 5
const float ACS_SENSITIVITY     = 0.040f; // ACS758-050B: 40 mV/A
const float R0                  = 0.020f; // Thevenin ESR (Ω)
const float R1                  = 0.030f; // Thevenin RC branch (Ω)
const float C1                  = 1140.f; // Thevenin RC branch (F)
const float CURRENT_DEADBAND    = 0.10f;  // dead-band for integration (A)
const int   REST_SECONDS        = 60;     // rest time before OCV correction
```

### SOC Forced-Reset Thresholds

| Pack Voltage | SOC Forced To |
|-------------|--------------|
| ≥ 14.5 V | 100 % |
| ≥ 14.2 V | 98 % (floor) |
| ≥ 14.0 V | 96 % (floor) |
| ≥ 3.475 V/cell after 60 s rest | 100 % |

---

## 🗂 Data Model

```kotlin
data class BatteryData(
    val voltage: Float = 0f,               // Pack voltage (V)
    val current: Float = 0f,               // Current magnitude (A, always ≥ 0)
    val temperature: Float = 0f,           // DS18B20 temperature (°C)
    val soc: Int = 0,                       // State of Charge (0–100 %)
    val remainingCapacityAh: Float = 0f,   // Remaining capacity (Ah)
    val remainingEnergyWh: Float = 0f,     // Remaining energy (Wh)
    val status: BatteryStatus = BatteryStatus.IDLE,
    val reservedEnergyWh: Int = 0          // Active reservation threshold (Wh)
) {
    val power: Float get() = voltage * current  // Derived: instantaneous power (W)
}
```

---

## 🔋 Energy Reservation

The energy reservation system allows a user to protect a defined block of Wh from use. Once activated:

1. The app sends `RESRV:<Wh>` to the ESP32 and displays a red badge on the dashboard.
2. The ESP32 monitors `remainingEnergyWh` each loop cycle.
3. When `remainingEnergyWh ≤ reservedEnergyWh`, the output relay opens, cutting power to the load.
4. The app sends `UNLOCK` to clear the reservation and re-enable the output.

This is useful for scenarios such as keeping emergency capacity for medical devices or communication equipment during an outage.

---

## 📦 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.09.00 | UI framework |
| Material 3 | (BOM) | Design system |
| Navigation Compose | 2.8.9 | Screen routing |
| Room | 2.7.0 | Local database |
| Retrofit + Moshi | 2.12.0 / 1.15.2 | HTTP + JSON |
| Coil Compose | 2.7.0 | Image loading |
| Coroutines | 1.10.2 | Async / Flow |
| Lifecycle ViewModel | 2.8.7 | MVVM |
| Firebase BOM | 34.12.0 | Firebase services |
| Roborazzi | 1.59.0 | Screenshot tests |

Full version catalogue: [`gradle/libs.versions.toml`](gradle/libs.versions.toml)

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Screenshot tests (Roborazzi)
./gradlew recordRoborazziDebug   # capture baseline
./gradlew verifyRoborazziDebug   # compare against baseline

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest
```

---

## 📋 Roadmap

- [ ] **BLE firmware module** — complete Arduino BLE server + command parser (in progress)
- [ ] **Analytics charts** — historical SOC, voltage, and power graphs (Room-backed)
- [ ] **Cycle count tracking** — detect and persist full charge/discharge cycles
- [ ] **Notifications** — push alerts for low battery, high temperature, reservation triggered
- [ ] **Multi-pack support** — connect to and switch between multiple PowerStation devices
- [ ] **Export CSV** — download telemetry history for offline analysis
- [ ] **Widget** — home-screen SOC gauge widget
- [ ] **iOS companion app** — SwiftUI port


```
MIT License

Copyright (c) 2025 PowerStation Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 🙏 Acknowledgements

- [Nordic Semiconductor](https://www.nordicsemi.com/) — Nordic UART Service (NUS) BLE profile
- [Adafruit Industries](https://www.adafruit.com/) — ADS1115, GFX, and SH110X Arduino libraries
- [Espressif Systems](https://www.espressif.com/) — ESP32 Arduino core and BLE stack
- Battery EKF design based on Plett, G.L. (2004) *Extended Kalman filtering for battery management systems of LiPB-based HEV battery packs*

---

<div align="center">

Made with ❤️ for the open hardware community

</div>
