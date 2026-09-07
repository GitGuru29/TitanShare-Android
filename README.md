# TitanShare Android 📱⚡💻

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.04-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-00A2FF.svg?style=flat)](https://m3.material.io)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg?style=flat)](https://android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-00E676.svg?style=flat)](https://android.com)
[![Arch Linux](https://img.shields.io/badge/Linux%20Ecosystem-Arch%20%7C%20Fedora%20%7C%20Ubuntu-1793D1.svg?style=flat&logo=arch-linux)](https://archlinux.org)
[![License](https://img.shields.io/badge/License-GPLv3%20%2F%20Proprietary-orange.svg?style=flat)](LICENSE)

**TitanShare Android** is a native companion application engineered in Kotlin and Jetpack Compose for the **TitanShare Linux Ecosystem**. It connects Android mobile devices to Linux workstations, transforming smartphones into wireless trackpads, hardware keyboards, remote telemetry dashboards, bidirectional file bridges, and low-latency screen casting monitors over the local area network.

---

## 📑 Table of Contents

- [Overview](#-overview)
- [System Architecture](#-system-architecture)
- [Key Features](#-key-features)
  - [1. Welcome & Onboarding](#1-welcome--onboarding)
  - [2. Zero-Config LAN Discovery (mDNS / NSD)](#2-zero-config-lan-discovery-mdns--nsd)
  - [3. Secure Pairing & Authentication](#3-secure-pairing--authentication)
  - [4. Bento-Style Remote Dashboard](#4-bento-style-remote-dashboard)
  - [5. Deep-Dive Hardware & Telemetry](#5-deep-dive-hardware--telemetry)
  - [6. Virtual Trackpad & Mouse Input](#6-virtual-trackpad--mouse-input)
  - [7. 32-Key Linux Keyboard](#7-32-key-linux-keyboard)
  - [8. Bidirectional File Transfer & PC Storage Browser](#8-bidirectional-file-transfer--pc-storage-browser)
  - [9. Real-Time Screen Mirroring](#9-real-time-screen-mirroring)
- [Network Protocol & IPC Specification](#-network-protocol--ipc-specification)
- [Daemon Storage & Directory Structure](#-daemon-storage--directory-structure)
- [Project Directory Structure](#-project-directory-structure)
- [Prerequisites & Requirements](#-prerequisites--requirements)
- [Building & Installation](#-building--installation)
  - [Using Android Studio](#using-android-studio)
  - [Using Gradle CLI](#using-gradle-cli)
  - [Deploying with ADB](#deploying-with-adb)
- [Troubleshooting & FAQ](#-troubleshooting--faq)
- [License & Authors](#-license--authors)

---

## 🎯 Overview

TitanShare provides an ecosystem experience between Android and Linux without relying on third-party cloud servers, external relays, or proprietary accounts. All interactions are direct, point-to-point, encrypted on LAN, and optimized for sub-millisecond responsiveness.

```
┌─────────────────────────────────────────────────────────┐
│                   TitanShare Android                    │
│      (Jetpack Compose • Coroutines • MediaProjection)    │
└────────────────────────────┬────────────────────────────┘
                             │
       LAN Protocol (TCP:9999 • UDP:5001 • mDNS:5353)
                             │
┌────────────────────────────▼────────────────────────────┐
│                    TitanShare Daemon                    │
│        (Native C++ • Arch Linux • uinput • systemd)     │
└─────────────────────────────────────────────────────────┘
```

---

## 🏗️ System Architecture

TitanShare Android follows modern Android architecture best practices with Clean Architecture and unidirectional data flow (UDF):

```
┌────────────────────────────────────────────────────────────────────────┐
│                               UI LAYER                                 │
│  Jetpack Compose Screens • Reusable Components • Cyberpunk Dark Theme  │
│ (DashboardScreen, FileTransferScreen, MirrorScreen, SystemDetails...)  │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │ StateFlow / Events
┌──────────────────────────────────┴─────────────────────────────────────┐
│                            VIEWMODEL LAYER                             │
│       AppViewModel (StateFlow, SharedFlow, Lifecycle Awareness)        │
└──────────────────────────────────▲─────────────────────────────────────┘
                                   │
┌──────────────────────────────────┴─────────────────────────────────────┐
│                           SERVICES & DATA                              │
│   ┌────────────────────┐ ┌────────────────────┐ ┌───────────────────┐  │
│   │   DaemonClient     │ │  DiscoveryManager  │ │   MirrorService   │  │
│   │ (TCP 9999 Sockets) │ │  (Android NSD mDNS)│ │ (MediaProjection) │  │
│   └────────────────────┘ └────────────────────┘ └───────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

##  Key Features

### 1. Welcome & Onboarding
- **First-Run Splash**: Onboarding walkthrough highlighting ecosystem capabilities.
- **Preference Persistence**: Automatically remembers completed onboarding and routes straight to Discovery on subsequent app launches.
- **Instant Shortcuts**: One-tap "Get Started" and "Skip" buttons.

### 2. Zero-Config LAN Discovery (mDNS / NSD)
- **Automatic Broadcast Listening**: Uses Android's `NsdManager` to listen for `_titanshare._tcp.` DNS-SD announcements.
- **Self-Healing Discovery**: Automated 30-second cycle to clear stale Android mDNS cache and resolve Linux daemons instantly.
- **TXT Record Parsing**: Automatically extracts daemon version strings, hostname, and metadata.

### 3. Secure Pairing & Authentication
- **Rolling 6-Digit PIN**: Handshake via authenticated challenge.
- **Visual Laptop Mockup**: High-fidelity animated illustration displaying target PC name and IP address.
- **QR Code Pairing**: Direct scanning support for quick pairing.
- **Stateful Reconnect**: Automatically re-establishes socket connections and stats polling when resuming from background.

### 4. Bento-Style Remote Dashboard
- **Quick Action Grid**: One-tap buttons for `Lock`, `Unlock`, `Sleep`, `Wake`, `Volume Up`, `Volume Down`, `Mute`, `Reboot`, and `Shutdown`.
- **Live Status Pill**: Real-time connected IP badge, battery percentage, and CPU temperature.
- **System Metric Cards**: Real-time meters for CPU utilization, RAM usage, storage consumption, and network bandwidth.

### 5. Deep-Dive Hardware & Telemetry
Organized into three dedicated sub-tabs:
- **Performance Tab**:
  - Live Task-Manager style CPU waveform canvas chart.
  - Per-Core utilization grid (supports up to 64 cores/threads).
  - RAM used vs. total with live percentage bars.
  - GPU load and memory telemetry.
- **Hardware Tab**:
  - Detailed CPU model name, core counts, and clock frequencies (GHz).
  - Motherboard brand, BIOS revision, and system chassis model.
  - Physical RAM layout and storage disk partitions.
- **Network Tab**:
  - Active network interface (`wlan0`, `eth0`), link throughput, local IP, and MAC address.

### 6. Virtual Trackpad & Mouse Input
- **Low Latency Gestures**: High-frequency relative mouse delta streaming (`MOUSE_MOVE:dx:dy`).
- **Smooth Scrolling**: Two-finger natural vertical scrolling (`MOUSE_SCROLL:delta`).
- **Clicks & Dragging**: Dedicated Left, Right, and Middle clicks with support for tap-to-click, touch-down, and touch-up drag events.

### 7. 32-Key Linux Keyboard
- **Text Injection**: Direct string transmission via `KEY_TYPE:<text>`.
- **Special Linux Keys**: 32 hardware keys including `Super` (Windows/Meta), `Ctrl`, `Alt`, `Shift`, `Esc`, `Tab`, `Backspace`, `Enter`, `Delete`, `Home`, `End`, `Page Up`, `Page Down`, Arrow navigation keys, and `F1` through `F12`.
- **Terminal Hotkeys**: Quick shortcuts for `Ctrl+C` (SIGINT), `Ctrl+Z` (SIGTSTP), `Ctrl+D` (EOF), and `Ctrl+L` (Clear).

### 8. Bidirectional File Transfer & PC Storage Browser
- **Dropzone File Picker**: Dashed glassmorphic dropzone with folder outline icon and tap-to-browse file selector.
- **High Throughput Streaming**: 128 KB buffered chunk streaming with live Mbps transfer speed calculation and linear progress bar.
- **Recent Transfers**: Comprehensive log with tailored file badges (Image avatar thumbnails, white document sheets, amber archive badges, video and audio icons).
- **Remote PC File Browser**: Browse and download files directly from the Linux PC's staging directory (`/var/lib/titanshare/send_to_android/` or `~/.local/share/titanshare/send_to_android/`).
- **MediaStore Integration**: Downloaded files are saved straight to `Downloads/TitanShare/` with scoped storage compliance (Android 10+).
- **AirDrop-Style Overlay**: Glowing animated transfer badge showing live filename and progress.

### 9. Real-Time Screen Mirroring
- **Hardware Accelerated Encoding**: Uses Android's `MediaProjection` API and `MediaCodec` (H.264 / AVC baseline) to capture and encode the phone's screen at 60 FPS.
- **Low-Latency Transport**: Streams encoded video packets directly to the daemon's mirror port (UDP/TCP Port 5001).
- **Foreground Service**: Runs as a foreground service with persistent notifications and auto-recovery.
- **Live Stream Diagnostics**: Displays real-time FPS, encoded resolution, frame count, dropped frames, and compression quality.

---

## 📡 Network Protocol & IPC Specification

TitanShare Android communicates with the Linux daemon on **TCP Port 9999** for control/commands/files and **Port 5001** for screen mirroring:

### Authentication & Handshake
| Client Request | Daemon Response | Description |
|---|---|---|
| `AUTH:<6-digit-pin>\n` | `AUTH_OK\n` | Successful handshake and session authorization |
| `AUTH:<wrong-pin>\n` | `AUTH_FAIL\n` | Authentication rejected |

### Remote Commands (`CMD:<action>`)
| Command Payload | Response | Description |
|---|---|---|
| `CMD:get_info\n` | `{"brand":"...","cpu_load":"...","cpu_cores_usage":[...],...}\n` | Returns full JSON telemetry snapshot |
| `CMD:lock\n` | `OK\n` | Locks the Linux desktop session (`loginctl lock-session`) |
| `CMD:unlock\n` | `OK\n` | Unlocks screen session |
| `CMD:sleep\n` | `OK\n` | Suspends Linux system (`systemctl suspend`) |
| `CMD:wakeup\n` | `OK\n` | Wakes screen / displays |
| `CMD:reboot\n` | `OK\n` | Reboots workstation (`systemctl reboot`) |
| `CMD:shutdown\n` | `OK\n` | Shuts down workstation (`systemctl poweroff`) |
| `CMD:volume_up\n` | `OK\n` | Increases system volume via PipeWire/PulseAudio |
| `CMD:volume_down\n` | `OK\n` | Decreases system volume |
| `CMD:mute\n` | `OK\n` | Toggles audio mute state |

### Input Emulation (No Response Expected for Max Performance)
| Command Payload | Description |
|---|---|
| `CMD:MOUSE_MOVE:<dx>:<dy>\n` | Relative pointer movement |
| `CMD:MOUSE_CLICK:<left\|right\|middle>\n` | Mouse button click |
| `CMD:MOUSE_DOWN:<left\|right\|middle>\n` | Mouse button press |
| `CMD:MOUSE_UP:<left\|right\|middle>\n` | Mouse button release |
| `CMD:MOUSE_SCROLL:<delta>\n` | Mouse wheel vertical scroll |
| `CMD:KEY_TYPE:<text>\n` | Unicode text entry |
| `CMD:KEY_PRESS:<keyname>\n` | Key event (`Return`, `BackSpace`, `F1`..`F12`, `Super_L`, etc.) |

### File Transfer Protocol (Android → Linux)
```
Android (Client)                                   Linux Daemon
       │                                                 │
       │──────── FILE_START:<name>:<bytes>\n ───────────>│
       │<─────── READY_FOR_FILE\n ───────────────────────│
       │                                                 │
       │──────── [128 KB Binary Chunks...] ─────────────>│
       │                                                 │
       │<─────── FILE_OK\n ──────────────────────────────│
       ▼                                                 ▼
```

### File Receive Protocol (Linux → Android)
```
Android (Client)                                   Linux Daemon
       │                                                 │
       │──────── CMD:push_file_list\n ──────────────────>│
       │<─────── {"files":[{"name":"..","size":..}]}\n ──│
       │                                                 │
       │──────── CMD:push_file:<filename>\n ────────────>│
       │<─────── FILE_PUSH:<name>:<bytes>\n ─────────────│
       │<─────── [128 KB Binary Chunks...] ──────────────│
       │                                                 │
       │ (Saved to /storage/emulated/0/Download/TitanShare)
       ▼                                                 ▼
```

### Screen Mirroring Protocol
| Command Payload | Response | Description |
|---|---|---|
| `CMD:START_MIRROR\n` | `{"type":"MIRROR_READY","port":5001}\n` | Allocates UDP/TCP video receiver on daemon |
| `CMD:STOP_MIRROR\n` | `OK\n` | Terminates mirror receiver on daemon |

---

## 📂 Daemon Storage & Directory Structure

On the Linux PC, TitanShare structures storage based on running permissions:

| Runner Context | Base Storage Path |
|---|---|
| **System Service (Root / systemd)** | `/var/lib/titanshare/` |
| **User Session (Normal User)** | `~/.local/share/titanshare/` |
| **Fallback** | `/tmp/titanshare/` |

### Key Directory Layout
- **`received_files/`**: Destination for files uploaded from Android.
- **`send_to_android/`**: Shared directory for files available for Android to browse and download.
- **`last_session.json`**: Persists pairing state and remembered devices.
- **`/run/titanshare/titanshare-pin.json`**: Ephemeral runtime pairing PIN monitored by Linux GUI.

---

## 📁 Project Directory Structure

```
TitanShare(Android)/
├── app/
│   ├── build.gradle.kts                   # App-level Gradle build configuration
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml        # Permissions (Network, NSD, MediaProjection, Foreground)
│           ├── java/com/titanshare/android/
│           │   ├── MainActivity.kt        # Main single-activity entrypoint
│           │   ├── data/
│           │   │   ├── mirror/            # Screen capture & video streamers
│           │   │   │   ├── ScreenMirrorCapture.kt
│           │   │   │   └── TcpMirrorStreamer.kt
│           │   │   ├── model/             # Domain data models
│           │   │   │   ├── Device.kt
│           │   │   │   └── SystemInfo.kt
│           │   │   └── network/           # Networking and socket clients
│           │   │       ├── DaemonClient.kt
│           │   │       └── DiscoveryManager.kt
│           │   ├── services/
│           │   │   └── MirrorService.kt   # Foreground service for screen capture
│           │   ├── ui/
│           │   │   ├── components/        # Reusable UI widgets (AirDropOverlay, GlassCard...)
│           │   │   ├── navigation/        # Navigation Graph and Route definitions
│           │   │   │   ├── NavGraph.kt
│           │   │   │   └── Screen.kt
│           │   │   ├── screens/           # Jetpack Compose UI Screens
│           │   │   │   ├── DashboardScreen.kt
│           │   │   │   ├── DiscoveryScreen.kt
│           │   │   │   ├── FileTransferScreen.kt
│           │   │   │   ├── KeyboardScreen.kt
│           │   │   │   ├── LinuxFilesScreen.kt
│           │   │   │   ├── MirrorScreen.kt
│           │   │   │   ├── PairingScreen.kt
│           │   │   │   ├── SystemDetailsScreen.kt
│           │   │   │   ├── TrackpadScreen.kt
│           │   │   │   └── WelcomeScreen.kt
│           │   │   └── theme/             # Cyberpunk theme, Colors, and Typography
│           │   │       ├── Color.kt
│           │   │       ├── Theme.kt
│           │   │       └── Type.kt
│           │   └── viewmodel/
│           │       └── AppViewModel.kt    # Unified UI state and reactive orchestration
│           └── res/                       # App icons, vectors, and XML resources
├── gradle/
│   ├── libs.versions.toml                 # Version Catalog (Dependencies & Plugins)
│   └── wrapper/                           # Gradle Wrapper definitions
├── build.gradle.kts                       # Root project build configuration
├── settings.gradle.kts                    # Module settings and repositories
└── README.md                              # Project documentation
```

---

## 📋 Prerequisites & Requirements

### Android Device
- **Android Version**: Android 8.0 Oreo (API 26) or higher (Target: Android 14 / API 34).
- **Network**: Wi-Fi connection on the same local subnet as the Linux PC.
- **Permissions**: MediaProjection permission (requested at runtime for screen mirroring).

### Linux PC Workstation
- **TitanShare Daemon**: Running `titanshare-daemon` (v2.0.0+).
- **mDNS Service**: `avahi-daemon` enabled and active (`systemctl enable --now avahi-daemon`).
- **Firewall Configuration**: Ensure the following ports are open in `ufw` or `firewalld`:
  - `9999/tcp` — Command & File Channel
  - `5001/udp` & `5001/tcp` — Screen Mirror Stream
  - `5353/udp` — mDNS / Avahi Discovery

---

## 🛠️ Building & Installation

### Using Android Studio
1. Clone or download the repository:
   ```bash
   git clone https://github.com/GitGuru29/TitanShare-Android.git
   ```
2. Open **Android Studio** (Hedgehog 2023.1.1 or newer).
3. Select **Open an Existing Project** and browse to the `TitanShare(Android)` directory.
4. Allow Gradle to sync dependencies.
5. Connect your Android device (with USB Debugging enabled) and click **Run ▶** (`Shift + F10`).

### Using Gradle CLI
```bash
# Navigate to the project root
cd "TitanShare(Android)"

# Build the Debug APK
./gradlew assembleDebug

# Build the Release APK
./gradlew assembleRelease
```

Generated APKs are located at:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### Deploying with ADB
```bash
# Install directly to connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.titanshare.android/.MainActivity
```

---

## ❓ Troubleshooting & FAQ

<details>
<summary><b>1. Discovery screen keeps scanning and does not find my PC</b></summary>

- Verify both devices are connected to the **exact same Wi-Fi network** (and not isolated guest networks).
- Check if `avahi-daemon` is running on Linux: `systemctl status avahi-daemon`.
- Ensure your Linux firewall permits mDNS: `sudo ufw allow 5353/udp` or `sudo firewall-cmd --add-service=mdns --permanent`.
- Verify the TitanShare daemon is running: `systemctl status titanshare` or `ps aux | grep titanshare`.
</details>

<details>
<summary><b>2. Pairing fails with "Wrong PIN or connection refused"</b></summary>

- Check the active PIN displayed on your Linux PC desktop GUI or in `/run/titanshare/titanshare-pin.json`.
- Note that the daemon refreshes the pairing PIN periodically for security.
- Ensure TCP port 9999 is accessible: `sudo ufw allow 9999/tcp`.
</details>

<details>
<summary><b>3. Screen Mirror displays a black screen or disconnects</b></summary>

- Grant the **Screen Recording / MediaProjection** permission when prompted on Android.
- Ensure UDP/TCP port 5001 is unblocked on your PC firewall: `sudo ufw allow 5001/udp && sudo ufw allow 5001/tcp`.
- Lower the mirror resolution in settings if using a low-bandwidth 2.4 GHz Wi-Fi link.
</details>

<details>
<summary><b>4. Where are received files saved on my Android device?</b></summary>

- Files transferred from the PC are saved to your phone's internal storage under:  
  📁 `Internal Storage / Download / TitanShare /`
- They will automatically appear in your Gallery / File Manager apps.
</details>

---

## 📜 License & Authors

Distributed under the **GPLv3 / Proprietary License**. See `LICENSE` for details.

Developed with for the **TitanShare Linux Ecosystem**.
