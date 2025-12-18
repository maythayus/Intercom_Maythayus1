# Intercom2SOS — Survival Pack (Android)

**Intercom2SOS** is an Android “survival pack” app: **Intercom / Push‑To‑Talk**, **SOS assistance** for life‑threatening situations, and enhanced **Xiaomi ecosystem compatibility** (phones + optional BLE ring/HR sensor, depending on your setup).

> Goal: provide a **fast and reliable workflow** to trigger an alert, share GPS location, and (when available) transmit an emergency voice message through an external USB/radio module.

---

## Key Features

### 1) Intercom (PTT)
- **Push‑To‑Talk control** through a connected **USB device** (host mode).
- Sends commands to the external controller (frequency, status, PTT on/off).

### 2) SOS mode (critical emergency)
- Retrieves **GPS position** (high accuracy when possible).
- Builds an **emergency message** (shareable text).
- Optional “on‑air” emergency transmission:
  - sends GPS to the USB controller
  - triggers **PTT down**
  - speaks a prebuilt emergency message via **Text‑To‑Speech**
  - releases **PTT up** when done
- Emergency number helper (**112** or **911** depending on country) via the phone dialer.

### 3) Survival Pack workflow
Designed to minimize steps under stress:
- Quick status checks: USB connected / BLE connected / GPS OK
- SOS **TEST** mode vs **REAL** mode
- One‑tap sharing of an emergency message (SMS/WhatsApp/etc.)

### 4) Xiaomi / Ring compatibility (BLE)
- Optional BLE heart‑rate support (standard Heart Rate GATT service).
- Heart rate (bpm) can be included in the emergency message (if available).

---

## Recommended “Survival Pack” Procedure

### TEST mode (always do this first)
1. Enable **Location/GPS**.
2. Connect the **USB module** (if you use radio/PTT features).
3. (Optional) Connect the **BLE ring/sensor**.
4. Run **SOS in TEST mode**:
   - checks GPS
   - sends data without real transmission

### REAL SOS (life‑threatening situation)
1. Ensure **USB module is connected** (if you rely on radio/PTT transmission).
2. Trigger **SOS**.
3. Optional actions:
   - **Call emergency services** (112/911)
   - **Share** the emergency message via your preferred app

---

## Permissions & Privacy

The app may use:
- **Location** (GPS) for SOS / sharing
- **Bluetooth (BLE)** for ring/sensor
- **Notifications** for foreground service (USB)
- **Audio / microphone** depending on intercom features

Data such as GPS and heart rate are used for on‑screen status and/or composing the emergency message.  
**Do not ship to production without a security/legal review** if you add automation (auto call/SMS).

---

## Tech Stack
- Kotlin / Android
- Jetpack Compose (+ potential XML parts)
- Hilt (DI)
- Google Play Services Location
- BLE (GATT heart rate)
- USB host + serial communication

---

## Important Disclaimer
This project involves **emergency scenarios**. Before real-world usage:
- Test with **TEST mode**
- Respect local laws/regulations
- Ensure Android runtime permissions and background/foreground restrictions are handled properly

---

## Build / Run
1. Open in Android Studio
2. Sync Gradle
3. Run on a device (grant required permissions)
4. Start with **TEST mode**
