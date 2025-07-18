# NewsWatch

NewsWatch is a scoped Android news reader using GNews for provider-ranked India/English headlines and remote search. It supports category browsing, durable bookmarks, offline saved-article detail, bounded home-feed caching, and native article actions.

## Project documents

- `AGENTS.md` — governing product and architecture decisions.
- `IMPLEMENTATION_PLAN.md` — approved phased implementation plan.
- `PROJECT_STATUS.md` — canonical implementation checklist.
- `HANDOFF.md` — current implementation state and resume instructions.

## Toolchain and setup

- Gradle Wrapper 8.10.2
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Java/Kotlin bytecode target 17; verified with JDK 23
- Android compile/target SDK 35; minimum SDK 26

On Windows, configure the Android SDK path in the ignored `local.properties` file. The checked-in wrapper is the supported Gradle entry point.

Configure the development GNews key through either the user Gradle properties file or the `GNEWS_API_KEY` environment variable:

```properties
GNEWS_API_KEY=replace-with-local-development-key
```

Credentials are consumed at build time and injected only at the OkHttp boundary. They are not part of domain models, UI state, logs, or source control. Release builds intentionally receive an empty key in this assignment configuration.

## Architecture

The project uses four pragmatic Gradle modules:

- `:app` — application, Hilt bootstrap, activity, theme, and root navigation.
- `:core` — domain models, repository contracts, errors, search processing, and bookmark use cases.
- `:data` — GNews Retrofit/OkHttp integration, DTO mapping, Room persistence, repositories, PagingSource, and RemoteMediator.
- `:feature-news` — Compose screens, ViewModels, navigation, article detail, WebView, and article actions.

Home data flows through `GNews -> RemoteMediator -> Room -> PagingSource -> Compose`. Room is the Home single source of truth. Refresh replacement is transactional and failed refreshes preserve the previous cache. The active feed is trimmed to 150 ranked items during Paging initialization. Remote search uses a network-only PagingSource and is not persisted. Bookmarks are stored independently of feed cleanup.

## User-visible behavior

- Home defaults to India (`IN`) and English (`en`) and uses GNews top-headlines ranking.
- Home supports category selection, Paging append, pull-to-refresh, stale-cache display, retry, and offline cached browsing.
- Search trims and normalizes input, requires two characters, debounces 300 ms, deduplicates queries, cancels obsolete requests, and is unavailable offline.
- Bookmarks remain available offline and can be searched locally.
- Saved articles retain enough metadata for native offline detail.
- Article detail supports sharing, an in-app WebView for “See original,” and external-browser fallback.
- Connectivity is advisory; actual request and article-action failures determine the displayed result.

## Verification

Standard debug verification:

```powershell
.\gradlew.bat clean test lint assembleDebug
```

Phase 8 release verification:

```powershell
.\gradlew.bat clean test lint assembleRelease
```

Outputs include:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Lint reports: `app/build/reports/` and module-specific `build/reports/`
- Unit-test reports: module-specific `build/reports/tests/`

Unit tests use MockWebServer, in-memory Room, Robolectric, coroutine test dispatchers, and fakes; they do not spend GNews quota. Manual live smoke requests should be deliberate and limited.

## Offline and cache behavior

The cached Home feed is restored after process restart and remains visible when refresh fails or the device is offline. A successful refresh replaces the active feed atomically. Bookmarks are never removed by feed refresh or the 150-item startup trim. Offline users may browse and search bookmarks, but remote catalogue search requires network access. Offline webpage rendering is not promised; the native detail metadata remains available for bookmarked articles.

## Known limitations and delivery trade-offs

- GNews development access is treated as approximately 100 requests/day, up to 10 articles/request, delayed news, and development-only usage.
- Release builds intentionally have no configured API key for this assignment; provide a permitted key only for local development/demo builds.
- The app stores article metadata and image URLs, not offline webpage HTML, media, or image blobs.
- No account synchronization, background notifications, permanent per-query search cache, or custom editorial ranking is included.
- Live provider smoke tests can be rate-limited with HTTP 429; deterministic local tests are preferred.