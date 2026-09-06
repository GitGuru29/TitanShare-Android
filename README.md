# TitanShare Android

An Android application built with Jetpack Compose for the **TitanShare Linux Ecosystem** — seamless remote control, system monitoring, file transfer, and real-time screen mirroring between your Android device and Arch Linux PC.

---

## 🌟 Key Features

| Screen | Description |
|---|---|
| 🚀 **Welcome / Onboarding** | Futuristic introduction screen; automatically appears on first launch and hides on subsequent opens |
| 📡 **Device Discovery** | Automatic LAN scanning for `_titanshare._tcp` daemons using mDNS / Avahi — no IP configuration needed |
| 🔐 **Pair Device** | High-tech pairing UI with interactive 6-digit PIN input, laptop illustration, and QR code support |
| 🏠 **Home / Dashboard** | Bento-style dashboard featuring quick actions, connected device status, and live system metrics |
| ⚙️ **System Details** | Deep-dive telemetry with sub-tabs (*Performance*, *Hardware*, *Network*), live Task-Manager CPU waveform graph, and 8-core CPU utilization grid |
| 🖱️ **Trackpad** | Full-screen touchpad with fluid gestures, multi-finger scrolling, and mouse clicks |
| ⌨️ **Keyboard** | Text input with 32 special Linux function keys, shortcuts, and key combinations |
| 📁 **File Transfer & Browser** | Bidirectional file sharing (Android ↔ Linux) and remote Linux file system browser |
| 🖥️ **Screen Mirroring** | Low-latency real-time phone screen streaming to your Linux PC |

---

## 📡 Protocol Summary

TitanShare Android communicates with the Linux daemon via TCP & UDP sockets:

```text
TCP Port :9999
AUTH:<6-digit-pin>\n        →  AUTH_OK\n
CMD:<command>\n             →  JSON response (e.g. get_info, volume_up, lock, sleep)
FILE_START:<name>:<bytes>\n →  READY_FOR_FILE\n → raw bytes → FILE_END\n → FILE_OK\n
CMD:START_MIRROR            →  {"type":"MIRROR_READY","port":5001}\n (UDP stream)
```

---

## 🛠️ System Requirements

- **TitanShare Daemon** running on your Linux PC (`systemctl start titanshare`)
- Both devices connected to the **same local Wi-Fi network**
- **Avahi / mDNS** enabled on Linux (`systemctl start avahi-daemon`)
- **Android 8.0+** (`minSdk 26`, `targetSdk 35`)

---

## 🚀 Building & Running

### Open in Android Studio
1. Launch **Android Studio** (Hedgehog or newer)
2. Select **File → Open** and choose the `TitanShare(Android)` project folder
3. Sync Gradle and press **Run ▶** to deploy to your Android device or emulator

### Build from Command Line (CLI)
```bash
# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
