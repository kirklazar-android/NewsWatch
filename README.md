# NewsWatch

NewsWatch is a scoped Android news reader using GNews for ranked India/English headlines and remote search.

Project documents:

- `AGENTS.md`: governing product and architecture decisions.
- `IMPLEMENTATION_PLAN.md`: approved phased implementation plan.
- `PROJECT_STATUS.md`: canonical implementation checklist.
- `HANDOFF.md`: current state and resume instructions.

## Toolchain

- Gradle Wrapper 8.10.2
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Java/Kotlin target 17; verified using JDK 23
- Android compile/target SDK 35, minimum SDK 26

The checked-in wrapper is the supported way to run Gradle. The Android SDK path belongs in ignored `local.properties`.

## GNews API key

Provide the development key either as a Gradle property or environment variable named `GNEWS_API_KEY`. The key is injected by the app module at the OkHttp boundary; it is not exposed to domain or presentation APIs and is never logged. Release builds intentionally receive an empty key in the current assignment configuration.

Example local Gradle property in the user Gradle properties file:

```properties
GNEWS_API_KEY=replace-with-local-development-key
```

Do not commit credentials or `local.properties`.

## Build and verification

On Windows with a valid Java installation:

```powershell
.\gradlew.bat clean test lint assembleDebug
```

Verified outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Lint report: `app/build/reports/lint-results-debug.html`
- Unit-test reports: `data/build/reports/tests/`

## Provider constraints

The selected GNews development tier is treated as approximately 100 requests/day, up to 10 articles/request, delayed news, and development-only use. Unit tests use MockWebServer and do not spend quota. Live smoke requests should be deliberate and limited.

## Architecture baseline

Home will use `GNews API -> RemoteMediator -> Room -> PagingSource -> Compose`. Search will use a network PagingSource. Bookmarks remain independent of the feed cache. Feature implementation begins only after the setup gate recorded in `PROJECT_STATUS.md` is green.