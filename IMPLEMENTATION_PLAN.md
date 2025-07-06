# NewsWatch Implementation Plan

This is the durable local copy of the approved implementation plan. `AGENTS.md` governs product and architecture decisions; `PROJECT_STATUS.md` records execution state.

## Delivery rules

- Implement phases in order and keep only one phase active unless independent test work is explicitly parallelized.
- Do not mark a phase complete until its build and test gate passes.
- Update `PROJECT_STATUS.md` at phase start, after material progress, after failed verification, and at completion.
- Keep GNews-specific details inside `:data`.
- Do not expose DTOs or Room entities to ViewModels or Compose.
- Preserve cached Home content when refresh fails.
- Keep bookmarks independent of feed cache and cache cleanup.
- Prefer deterministic tests over live GNews calls.

## Phase 1 - Foundation and provider validation

Status at handoff: setup and automated provider-contract gate complete; one live GNews smoke request deferred.

Actions:

- Configure the four-module project and dependency/toolchain baseline.
- Configure secure local GNews-key injection.
- Implement top-headlines and search Retrofit contracts.
- Implement DTO parsing, URL normalization, stable IDs, and domain error mapping.
- Test request parameters, parsing, missing/malformed data, API-key behavior, and provider errors.

Completion criteria:

- Wrapper, Android SDK, modules, plugins, and dependencies resolve.
- No provider key appears in source, logs, domain models, or presentation APIs.
- Tests, lint, and debug assembly pass.
- A deliberate live smoke request confirms a mapped India/English response when a key is available.

Test gate:

```powershell
.\gradlew.bat clean test lint assembleDebug
```

## Phase 2 - Domain design

Actions:

- Finalize `Article`, `FeedRequest`, bookmark, pagination, feed-state, and domain-error models.
- Define stable request-key and article-identity semantics.
- Finalize provider-independent `NewsRepository` and `BookmarkRepository` contracts.
- Add focused use cases only where they express business rules.
- Add fake/in-memory repositories for ViewModel and contract tests.

Completion criteria:

- `:core` public APIs do not reference Retrofit, Room, GNews DTOs, UI types, or data implementations.
- Home, remote search, bookmarks, local bookmark search, article lookup, and bookmark-state observation have explicit contracts.
- Retryable and non-retryable errors are representable.

Testing:

- Request-key equality and country/language/category changes.
- Canonical URL identity and provider-ID preference.
- Domain error retryability.
- Fake repository paging and bookmark-state behavior.

## Phase 3 - Persistence and repositories

Actions:

- Implement `cached_articles`, `bookmarked_articles`, and single-row `feed_metadata`.
- Add indexes for IDs, feed position, URLs, request state, title, and source lookup.
- Implement DAOs for Room paging, lookup, metadata, bookmark observation/search, batch writes, cache replacement, and startup trimming.
- Implement schema version 1, schema export, and migration strategy.
- Implement entity/domain mappers and repository foundations.

Completion criteria:

- Feed rows are ordered by provider position.
- Successful refresh replacement is atomic.
- Failed replacement cannot erase the previous cache.
- Startup maintenance retains exactly 150 ranked feed rows.
- Feed maintenance never modifies bookmarks.

Testing:

- DAO insertion, upsert, ordering, lookup, deletion, and Flow invalidation.
- Transactional replacement and rollback.
- Startup trim and bookmark independence.
- Schema export, recreation, and migration behavior.

## Phase 4 - Home feed paging

Actions:

- Implement `NewsRemoteMediator`.
- Use Room `PagingSource` as Home's only UI data source.
- Implement refresh, append, unsupported prepend, metadata, and end-of-pagination.
- Preserve stale cache during network failures.
- Implement request-key changes without clearing previous cache before success.
- Implement the 30-minute launch-refresh threshold.

Completion criteria:

- Cold launch fetches and stores ranked headlines.
- Cached/offline launch shows Room data.
- Successful refresh atomically replaces the active feed.
- Failed refresh keeps existing content.
- Append retry never discards loaded rows.

Testing:

- Initial refresh, replacement, failed refresh, append, failed append, empty page, end-of-pagination, prepend, request-key change, stale launch, and offline cache.

## Phase 5 - Bookmarks and article detail

Actions:

- Implement durable bookmark repository and use cases.
- Observe bookmark state as Room-backed Flows across all surfaces.
- Implement local bookmark search.
- Implement one native detail destination for cached and bookmarked articles.
- Implement share, Custom Tabs, and external browser fallback.

Completion criteria:

- Bookmarking from any surface updates all observers.
- Bookmarks survive restart, refresh, and cache cleanup.
- A bookmarked article opens native detail offline.
- Network-dependent actions fail clearly and safely.

Testing:

- Bookmark insert, observe, search, delete, restart, and cleanup independence.
- Offline detail.
- Share intent, missing URL, Custom Tabs, browser fallback, and action failures.

## Phase 6 - Remote search

Actions:

- Implement a network-only search `PagingSource`.
- Normalize query input, enforce minimum length, debounce, deduplicate, and cancel stale queries with `flatMapLatest`.
- Implement result, loading, empty, refresh-error, append-error, retry, and offline states.
- Keep remote results out of Room.

Completion criteria:

- Blank/short queries do not call GNews.
- Duplicate queries do not repeat requests.
- Older responses cannot overwrite newer queries.
- Offline remote search is unavailable while bookmark search remains usable.

Testing:

- Query normalization, debounce, deduplication, cancellation, paging, empty results, provider failures, retries, and offline behavior.

## Phase 7 - Presentation and navigation

Actions:

- Implement Home, Search, Bookmarks, and Article Detail.
- Add bottom navigation, categories, pull-to-refresh, stable list keys, placeholders, and all load states.
- Preserve stale content during refresh.
- Add dark theme, font scaling, content descriptions, touch targets, and responsive phone layouts.
- Add detail deep links when supported cleanly by the app shell.

Completion criteria:

- The primary journey has no dead ends.
- Loading, empty, content, stale, offline, and failure states are distinct.
- Navigation and bookmark state remain consistent.
- Core layouts survive dark mode and larger font scales.

Testing:

- ViewModel state transitions.
- Compose rendering and actions.
- Navigation, back stack, and deep links.
- Dark theme, font scale, labels, descriptions, touch targets, and contrast.

## Phase 8 - Hardening and delivery

Actions:

- Validate process death, restart, startup trimming, refresh threshold, quota-aware retries, credential safety, and bounded image caching.
- Ensure logs never contain secrets or raw provider errors.
- Complete setup, architecture, offline behavior, trade-offs, limitations, and test documentation.
- Run clean-install, cached-restart, offline, failure, and demo scenarios.

Completion criteria:

- All governed acceptance criteria pass from a clean checkout.
- Debug build, tests, lint, and release compilation pass.
- Documentation matches the implementation.
- No known blocker remains in Home, Search, Bookmarks, or Detail.

Final test matrix:

- Network success, timeout, no connectivity, authentication, quota, server, parsing, and empty responses.
- Refresh failure with and without cache.
- Append failure and retry.
- Restart with cache and bookmarks.
- Cache cleanup with bookmarks.
- Dark mode, font scaling, accessibility, and article-action failures.

## Final acceptance gate

- GNews-ranked India/English Home feed loads and paginates.
- Room is Home's single source of truth.
- Refresh replacement is transactional and failure-safe.
- Cached Home restores after restart and works offline.
- Startup maintenance retains 150 articles.
- Remote search is debounced, cancellable, paginated, and non-persistent.
- Bookmark search works locally.
- Bookmarks persist independently and open native detail offline.
- Share, Custom Tabs, and browser fallback handle failures.
- Repository, DAO, mediator, ViewModel, navigation, and critical Compose tests pass.
- Setup, API-key configuration, quotas, architecture, offline behavior, trade-offs, and limitations are documented.