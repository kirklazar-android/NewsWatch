# NewsWatch Implementation Status

This is the canonical phase-by-phase implementation checklist. Update it whenever work begins, completes, or becomes blocked. Governing product and architecture decisions remain in `AGENTS.md`.

Status values: `[x]` complete, `[~]` in progress, `[ ]` pending, `[!]` blocked.

## Phase 1 — Project foundation and provider validation

Status: Complete — 2026-07-24

- [x] Four modules configured: `:app`, `:core`, `:data`, `:feature-news`.
- [x] Version catalog created for plugins and dependencies.
- [x] Gradle 8.10.2 wrapper installed and verified.
- [x] AGP 8.7.3, Kotlin 2.0.21, KSP, Compose, Hilt, Paging, Room, Retrofit, Moshi, OkHttp, Coroutines, and Coil configured.
- [x] Java/Kotlin bytecode target set to 17; build runs on installed JDK 23.
- [x] Android SDK 35 and Build Tools 35 resolved through ignored `local.properties`.
- [x] Debug/release build types, ProGuard baselines, test options, packaging rules, and Room schema export configured.
- [x] GNews top-headlines and search Retrofit contracts implemented.
- [x] India (`IN`), English (`en`), page size 10, and provider limits centralized.
- [x] API key sourced from a Gradle property or environment variable, injected at the OkHttp boundary, omitted from domain/UI APIs, and blocked when blank.
- [x] Moshi DTO adapters, URL normalization, stable article IDs, and provider-independent mapping implemented.
- [x] Network, HTTP, quota, parsing, and unknown error classification implemented.
- [x] Provider contract, request construction, parsing, mapping, API-key, and quota tests added.
- [x] Setup documentation and ignored local configuration added.

Verification gate:

- [x] `gradlew.bat clean` (passed after stopping stale Gradle daemon)
- [x] `gradlew.bat test` — 20 variant test executions, 0 failures, 0 skipped.
- [x] `gradlew.bat lint` — passed; report at `app/build/reports/lint-results-debug.html`.
- [x] `gradlew.bat assembleDebug` — passed; APK at `app/build/outputs/apk/debug/app-debug.apk`.
- [x] `git diff --check` — passed.
- [!] Live GNews smoke: top-headlines passed with 10 articles; search reached GNews but returned HTTP 429 rate-limit response. Do not retry until quota resets.

## Phase 2 — Domain design

Status: Complete — 2026-07-24

- [x] Domain models, normalized feed request semantics, request keys, and domain errors finalized.
- [x] Provider-independent news and bookmark repository interfaces finalized.
- [x] Domain model, request-key, error, and fake-repository tests added.

Completion gate: :core:test and :core:assembleDebug passed; no Room, Retrofit, GNews, or provider-specific types leak into :core public source.

## Phase 3 — Persistence and data layer

Status: Complete — 2026-07-24

- [x] Room entities, DAOs, indexes, database version 1, and schema export configured.
- [x] Feed metadata, transactional replacement/append, startup trim to 150, and durable bookmark storage/search implemented.
- [x] Entity/domain mappers, Hilt database/repository foundations, and persistence tests added.

Completion gate: :data:test and :data:assembleDebug passed for debug/release; tests prove replacement, append ordering, startup trimming, metadata, bookmark search, and bookmark independence.

## Phase 4 — Home feed paging

Status: Complete — 2026-07-24

- [x] Room-backed home PagingSource, HomeNewsRepository, and NewsRemoteMediator implemented.
- [x] Refresh, append, unsupported prepend, request-key changes, stale-cache preservation, and 30-minute refresh policy implemented.
- [x] Mediator tests cover refresh, append, fresh/stale initialization, prepend, request changes, offline/error preservation, and end-of-pagination.

Completion gate: :data:test and :data:assembleDebug passed for debug/release; Home data flows from GNews through RemoteMediator into Room and then PagingSource.

## Phase 5 — Bookmarks and article detail

Status: Complete — 2026-07-24

- [x] Bookmark use cases, Room-backed cross-surface state, local bookmark search, save/remove, and bookmarked-article lookup implemented.
- [x] Native detail state/action boundary implemented for bookmarked offline content.
- [x] Share, Custom Tabs preference, external-browser fallback, missing-URL, no-handler, and launch-failure handling implemented.
- [x] Persistence, use-case, offline-bookmark, and intent/action tests added.

Completion gate: core, data, and feature tests pass for debug/release; bookmarks remain independent of feed maintenance and article actions fail safely.

## Phase 6 � Remote search

Status: Complete � 2026-07-24

- [x] Implemented network-only GNews search PagingSource; search results are not persisted.
- [x] Query processing trims/normalizes, enforces minimum length 2, debounces 300 ms, deduplicates, cancels obsolete searches, and gates remote search while offline.
- [x] Added paging, next/end-key, network-error, normalization, debounce, deduplication, short-query, and offline tests.

Completion gate: :core:test, :data:test, :feature-news:test, :data:assembleDebug, and :feature-news:assembleDebug passed.

## Phase 7 � Presentation and navigation

Status: Complete � 2026-07-24

- [x] Wired Home, Search, Bookmarks, and native Detail Compose destinations through Navigation Compose.
- [x] Wired app-level Hilt repositories into feature ViewModels and UI flows.
- [x] Added article lookup, durable bookmark actions, original-link, and share actions.
- [x] Added category selection, Paging refresh/append loading states, stale-cache refresh errors, retry controls, empty states, search validation, bookmark fallbacks, and accessibility semantics.
- [x] Confirmed RemoteMediator remains behind the Room-backed HomeNewsRepository; Compose observes PagingData only.

Completion gate: :core:test, :data:test, :feature-news:test, :app:testDebugUnitTest, and :app:assembleDebug passed.

## Phase 8 — Hardening and documentation

Status: In progress — 2026-07-25

- [x] Release build, credential injection, authentication/quota classification, and raw-error log/UI safety validated.
- [x] README architecture, setup, offline behavior, limitations, and test documentation completed.
- [ ] Validate process restart, bounded image-cache behavior, and end-to-end demo scenarios on a device/emulator.

Completion gate: full acceptance matrix passes from a clean checkout and no governed rule has changed without an explicit decision record.


