# CubeLens

Android app for scanning a Rubik's cube with the camera, solving it with the Kociemba algorithm, and timing solves with WCA-style rules.

## Features

- **Scan** — Capture all 6 faces via CameraX; live color preview; 6-face cube net; gallery import; manual input; adjustable color calibration
- **Solve** — Kociemba two-phase solver with step-by-step 3D animation
- **Timer** — WCA inspection (15 s), hold-to-start / touch-to-stop, +2 / DNF penalties
- **History** — Best, Ao5, Ao12, Ao100, CSV export, solution replay
- **Settings** — Theme, camera lens, inspection toggle, data reset
- **i18n** — English and Simplified Chinese

## Requirements

- Android 8.0+ (API 26)
- JDK 21 (for building)
- Android SDK 35

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Tests

```bash
./gradlew test
```

## Project layout

```
app/src/main/java/com/cubelens/
├── camera/          ColorDetector (HSV)
├── solver/          KociembaSolver, CubeState, Move
├── data/            Room DB, DataStore preferences
├── ui/              Compose screens
└── util/            ScrambleUtils, StatsUtils
```

## Timer (WCA-style)

1. **Hold** the screen (~0.3 s) and **release** to start inspection (if enabled) or the solve timer
2. During inspection: **release** to start early, or wait for auto-start at 15 s
3. Start after 15 s (manual): **+2**; after 17 s without starting: **DNF**
4. While running: **touch** to stop

## License

See repository for license information.
