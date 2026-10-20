# 🚗 Apni Parking — Smart Live Parking Tracking Platform

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Maps](https://img.shields.io/badge/Google%20Maps-SDK-EA4335.svg?style=flat&logo=googlemaps)](https://developers.google.com/maps)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28.svg?style=flat&logo=firebase)](https://firebase.google.com/)

**Apni Parking** is a modern, high-performance Android application designed to solve real-time parking congestion in **Udaipur, Rajasthan**. It empowers commuters to discover live vacant slots, navigate through motorable streets using smart road-matching algorithms, and reserve parking spots seamlessly.

---

## ✨ Features

- **🎯 Live Vacant Slot Tracking:** Real-time occupancy tracking for key parking hubs across Udaipur.
- **🛣️ Smart Road-Following Routing (Magnet FX):** Advanced waypoint snapping ensures routes follow actual motorable streets without cutting through alleys, buildings, or pedestrian markets.
- **🏍️ 2-Wheeler vs. 4-Wheeler Modes:** Tailored route optimization and ETA estimates depending on vehicle choice.
- **🛰️ Satellite & Normal Map Layers:** Instant layer toggling between standard road maps and high-resolution satellite imagery.
- **⚡ High-FPS Pre-Warming Engine:** Background asynchronous vector map preloading ensures silky smooth 60–120 FPS navigation.
- **⏱️ Live 15-Minute Slot Hold:** Atomic slot reservations with real-time countdown timers.
- **🔒 Privacy-First Location Policy:** First-launch disclosure explaining exact GPS usage (navigation only, zero tracking).
- **🔑 Flexible Authentication:** Google Sign-In, Email/Password, and Instant Guest Demo access.

---

## 📍 Udaipur Parking Coverage

| Hub | Location | Capacity |
|---|---|---|
| **Surajpole Main Circle** | Near Surajpole Gate | 40 Slots |
| **Bapu Bazar Entry** | Bapu Bazar Main Market | 25 Slots |
| **Delhi Gate Connector** | Delhi Gate Junction | 50 Slots |
| **Station Road Multilevel** | City Railway Station Link | 80 Slots |
| **Hathipole Market** | Hathipole Main Road | 45 Slots |
| **Chetak Circle Public Parking** | Chetak Circle | 60 Slots |
| **Town Hall Link** | Town Hall Road | 35 Slots |

---

## 🛠️ Tech Stack & Architecture

- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Language:** Kotlin 1.9+
- **Navigation:** Jetpack Navigation Compose
- **Maps:** Google Maps Compose SDK (`maps-compose:4.4.1`)
- **Location:** Google Play Services Location (1.5s balanced GPS polling)
- **Backend & Auth:** 
  - Firebase Authentication (Google Sign-In, Email/Password, Anonymous)
  - Cloud Firestore (Real-time snapshots & atomic transactions)
- **Asynchronous Execution:** Kotlin Coroutines & Reactive StateFlow

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog / Iguana / Koala or newer
- JDK 17
- Android SDK 34 (minSdk 24)

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/Naresh-sh/Apni-parking.git
   cd Apni-parking
   ```

2. Open the project in Android Studio or compile via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

3. Output APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
