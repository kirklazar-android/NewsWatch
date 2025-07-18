# NewsWatch Development Handoff

Last updated: 2026-07-24

## Purpose

This document gives the next engineer or agent enough context to resume NewsWatch without rediscovering product decisions, setup state, or implementation order.

Use the project documents in this order:

1. `AGENTS.md` - governing product and architecture decisions.
2. `IMPLEMENTATION_PLAN.md` - approved phased implementation plan.
3. `PROJECT_STATUS.md` - canonical phase-by-phase progress checklist.
4. `README.md` - local setup and verification commands.

If these documents conflict, `AGENTS.md` governs. Reconcile the plan and status before continuing.

## Product baseline

NewsWatch is a scoped Android news reader. Home shows GNews provider-ranked headlines for India (`IN`) in English (`en`), with category browsing. The planned app includes:

- Room-backed paged Home with startup/offline restoration.
- Remote, debounced, cancellable search without persistent query caches.
- Durable bookmarks independent of feed-cache cleanup.
- Native offline article detail for bookmarked content.
- Share, Custom Tabs, and external browser actions.
- Dark theme, font scaling, accessibility, and explicit loading/error/empty/stale states.

Fixed decisions:

- GNews is the v1 provider.
- Home uses `top-headlines`; search uses `search`.
- Provider page size is 10.
- Startup feed retention is 150 articles.
- Feed refresh becomes due after 30 minutes.
- Local bookmark search is included.
- Read original uses the in-app WebView; Open in browser uses the external default browser.

## Current implementation state

### Phase 1: foundation and provider contracts

The project setup and automated Phase 1 gate are complete.

Implemented:

- Four Gradle modules: `:app`, `:core`, `:data`, and `:feature-news`.
- Version catalog and project-owned Gradle 8.10.2 wrapper.
- AGP 8.7.3, Kotlin 2.0.21, KSP, Compose, Hilt, Paging 3, Room, Retrofit, Moshi, OkHttp, Coroutines, and Coil configuration.
- Android SDK 35 / minimum SDK 26; Java and Kotlin bytecode target 17.
- Debug/release build types, ProGuard baselines, tests, and Room schema export configuration.
- Local `GNEWS_API_KEY` input, app-to-data Hilt binding, and OkHttp API-key injection.
- GNews Retrofit contracts for top headlines and search.
- Moshi DTOs, URL normalization, stable fallback IDs, mapping, and domain error classification.
- MockWebServer request tests, parsing tests, mapping tests, missing-key tests, and quota tests.

Not implemented:

- Room entities, DAOs, database, migrations, and repositories.
- `RemoteMediator`, search `PagingSource`, cache maintenance, and refresh policy.
- Bookmark persistence and local bookmark search.
- ViewModels, Compose features, navigation, detail actions, and accessibility behavior.
- Phase 2 through Phase 8 tests.

The app currently launches a minimal `NewsWatch` text screen. It is a verified foundation, not a functional news reader.

## Verification evidence

This clean verification command passed:

```powershell
.\gradlew.bat clean test lint assembleDebug
```

Results:

- Unit tests: 20 variant executions, 0 failures, 0 skipped.
- Android lint: passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Lint report: `app/build/reports/lint-results-debug.html`.
- `git diff --check`: passed.

Top-headlines live smoke passed with 10 returned articles. Search reached GNews but returned HTTP 429; wait for quota reset before retrying. The Phase 6 module tests and debug assemblies now pass; the remaining live-only limitation is the GNews search HTTP 429 noted above.

## Local environment

- Windows 11.
- JDK 23 at `C:\Program Files\Java\jdk-23`.
- Android SDK at `C:\Users\gagan\AppData\Local\Android\Sdk`.
- Android Platform 35 and Build Tools 35.0.0.

`local.properties` is ignored and stores the machine-specific SDK path. Credentials must not be committed.

## Known constraints

- Treat GNews development access as approximately 100 requests/day, 10 articles/request, delayed news, and development-only usage.
- Prefer MockWebServer tests to preserve quota.
- Release builds currently receive an empty GNews key to avoid embedding a development credential.
- Git history now contains phase-oriented commits through the Phase 7 UI and paging fixes.
- Build outputs and local configuration must remain ignored.

## Next implementation action

Phase 8 hardening is in progress. The clean test/lint/release gate passes; remaining work is manual process-restart/offline/demo validation before final acceptance.

Phase 1 live search smoke remains rate-limited by GNews HTTP 429 and should be retried only after quota reset.

## Resume checklist

- Read `AGENTS.md`.
- Read `IMPLEMENTATION_PLAN.md`.
- Check `PROJECT_STATUS.md` for the active phase.
- Run `git status --short` and preserve unrelated changes.
- Run the setup verification command before changing architecture.
- Update `PROJECT_STATUS.md` whenever work starts, completes, fails verification, or changes scope.



