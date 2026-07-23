# TitanShare Android

Remote control app for the **TitanShare Linux daemon** — control your Arch Linux PC from your Android phone with no QR codes, just automatic LAN discovery + PIN pairing.

## Features

| Screen | What it does |
|---|---|
| **Discovery** | Auto-finds `_titanshare._tcp` daemons via mDNS — no IP needed |
| **Pairing** | Enter the 6-digit PIN shown on your Linux screen |
| **Dashboard** | Live system stats (CPU, RAM, Disk, Temp, Battery) + quick actions |
| **Trackpad** | Full-screen touchpad, scroll mode, left/right/middle click |
| **Keyboard** | Type text + 32 special keys (function keys, combos, arrows) |
| **File Transfer** | Send any file from phone to Linux via the daemon |

## Protocol summary

```
TCP :9999
AUTH:<6-digit-pin>\n   →  AUTH_OK\n
CMD:<command>\n        →  optional response\n
FILE_START:<name>:<bytes>\n → READY_FOR_FILE\n → raw bytes → FILE_END\n → FILE_OK\n
```

## How to open in Android Studio

1. Open **Android Studio** (Hedgehog or newer)
2. **File → Open** → select this folder (`TitanShare(Android)/`)
3. Android Studio will download Gradle and sync automatically
4. Connect your Android device (USB or wireless) and hit **Run ▶**

> **minSdk 26** (Android 8.0+)  
> **targetSdk 35** (Android 15)

## Requirements

- **TitanShare daemon** running on your Linux PC (`systemctl start titanshare`)
- Both devices on the **same Wi-Fi** network
- **Avahi/mDNS** enabled on Linux (`systemctl start avahi-daemon`)

## Build from CLI (optional)

```bash
# If you have Android SDK + Gradle installed:
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```
