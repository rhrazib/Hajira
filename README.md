# Hajira — Geo-Fenced Attendance

Native Android (Kotlin + Jetpack Compose) app. The user saves the office location with one tap, then can
only mark attendance while standing within **50 m** of it. A live ring shows the distance in real time.

## Project structure / approach

**Clean Architecture + MVVM**, with a strict one-way dependency: `presentation → domain ← data`.

```
com.hajira.app
├── domain/          Pure Kotlin. No Android imports.
│   ├── model/       Coordinates, LocationFix, Proximity, results
│   ├── policy/      AttendancePolicy (radius, accuracy, time window), ProximityEvaluator
│   ├── geo/         GeoMath (haversine)
│   ├── repository/  Interfaces: Office / Attendance / Location
│   └── usecase/     SetOfficeLocation, ObserveProximity, MarkAttendance, ...
├── data/            DataStore repositories + FusedLocationRepository (Play Services)
├── presentation/    AttendanceViewModel, UiState/Events, Compose screen + components
└── di/              AppContainer (manual DI composition root)
```

- **`AttendanceViewModel`** combines use-case flows (`office`, `proximity`, `attendance status`, permission
  access) into one immutable `StateFlow<AttendanceUiState>`; one-shot messages go through a `Channel`.
- **`MarkAttendanceUseCase`** never trusts the button state: it re-checks the time window and today's record
  and takes a **fresh GPS fix** before saving.
- GPS updates are only collected while location access is `READY`.
- Persistence: Jetpack DataStore (office coordinates, last check-in time).
- Map: OpenStreetMap via osmdroid (no API key) with the 50 m geofence drawn on it.

### Behaviour notes
- Fixes less accurate than 50 m still show the distance but cannot unlock check-in ("WEAK SIGNAL").
- Handles: permission denied, permanently denied, approximate-only location, GPS off, mocked location
  (switchable in `AttendancePolicy.blockMockLocation`), no GPS fix.
- The 09:00–10:30 window from the design is implemented but **off by default**
  (`AttendancePolicy.enforceTimeWindow`), because the written brief doesn't require it and it would lock
  reviewers out of testing. Set it to `true` to enforce it and show the footer.
- "Reset today's check-in (demo)" appears after check-in so the flow can be retried.

## Generative AI usage

I used Claude (Anthropic) as a pair-programmer and reviewer. I own the design decisions and reviewed every change.

First version was developed with AI assistance, followed by a review against the assessment requirements.
Refactoring and completion were done with AI assistance while maintaining a Clean Architecture + MVVM approach.
Essential Prompts
"Please review Task 1 and let me know if it is ready for submission."
"Complete any remaining requirements for Task 1 so I can submit it. Please maintain a clean architecture and MVVM approach and ensure all assessment requirements are properly addressed."
AI-assisted: scaffolding, the domain/use-case split, unit test cases, refactoring suggestions, and README draft.
Done by me: requirement analysis, architecture choices, running and testing on a real device, and final review.
## How to run

```bash
git clone https://github.com/rhrazib/Hajira.git
cd Hajira
./gradlew assembleDebug        # or open the folder in Android Studio (Koala+ / JDK 17)
./gradlew installDebug         # with a device or emulator connected
./gradlew test                 # unit tests
./gradlew assembleRelease      # APK at app/build/outputs/apk/release/
```

Requirements: Android 8.0+ (API 26), Google Play Services (for Fused Location), internet for map tiles only.
To sign the release build with your own key, create a git-ignored `keystore.properties` with
`storeFile`, `storePassword`, `keyAlias`, `keyPassword`; otherwise the debug key is used.

**Testing on an emulator:** Extended controls → Location → set a point, tap *Set Office Location*, then move
the pin ~100 m away to see *Out of range*, and back to unlock check-in.

## Download

Signed release APK (Android 8.0+): see the [Releases page](https://github.com/rhrazib/Hajira/releases/latest).

## Tests

Unit tests (JUnit) cover haversine math, the policy, the proximity evaluator, and the three use cases with
fake repositories (in-range / out-of-range / weak accuracy / already checked in / yesterday's record /
no office / no fix / permission missing / time window).

## Screenshots

| Office not set | In range | Checked in |
|---|---|---|
| ![](docs/screenshots/office_not_set.jpg) | ![](docs/screenshots/in_range.jpg) | ![](docs/screenshots/checked_in.jpg) |
