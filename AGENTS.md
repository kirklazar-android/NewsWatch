# NewsWatch Project Governance

This document is the governing baseline for the NewsWatch Android news reader. Future implementation, design, and product decisions must preserve these principles unless an explicit decision record supersedes them.

## Product purpose and scope

NewsWatch is a scoped Android news reader, not an unfiltered global-news stream. It presents provider-ranked headlines scoped by region and optionally category, supports remote news search, durable bookmarks, offline access to saved article metadata, and a bounded startup feed cache.

The project is optimized for a complete, testable delivery within a 48-hour assignment window. Simplicity, reliability, and clear boundaries take priority over speculative extensibility.
## Finalized news provider

GNews is the selected primary news provider for both the ranked home feed and remote search. This provider choice is part of the project baseline and must not change incidentally during implementation.

- Home endpoint: `GET /api/v4/top-headlines`.
- Search endpoint: `GET /api/v4/search`.
- Home parameters: `country`, `language`, `category`, `page`, and `max`.
- Search parameters: `query`, `language`, `country`, `page`, and `max`.
- Default home scope: India (`IN`), English language, with category-based browsing.
- GNews ranking remains authoritative; the app must not invent a competing ranking algorithm.
- The selected free tier is understood to provide approximately 100 requests/day, up to 10 articles/request, delayed news, and development-only usage. These constraints must influence testing, retries, demos, and documentation.
- API keys must stay out of source control and out of UI/domain models.

GNews-specific DTOs, parameters, quota details, and response quirks stay behind the data/repository boundary so a future provider can be evaluated without rewriting presentation logic.

## Non-negotiable product behavior

- Home uses the provider's ranked/top-headlines endpoint. Do not invent a popularity, breaking-news, or recency ranking algorithm.
- Home is scoped by a defined region/language and optional category. The exact defaults remain a product decision, not an incidental implementation detail.
- Home data is delivered through Paging 3 with a Room-backed `PagingSource` and `RemoteMediator`.
- Room is the single source of truth for the home feed. `RemoteMediator` writes network results to Room; it does not return article rows directly to the UI.
- A refresh replaces the active feed cache only after the new request succeeds, and the replacement is transactional.
- A failed refresh preserves the old cached content and exposes a stale/error state with an explicit retry.
- Cached home content is shown on restart and when offline. Bookmarks must not silently substitute for the home feed.
- The active feed cache may grow while paging. On startup, trim it to one chosen limit in the 100–200 item range, ordered by provider ranking position.
- Do not depend on an Android app-close callback to clear data.
- Remote search uses a network `PagingSource`, not `RemoteMediator`, and is not persistently cached per query.
- Search input must trim, enforce a minimum query length, debounce, use `distinctUntilChanged`, and cancel obsolete requests through `flatMapLatest` or equivalent semantics.
- Offline users may browse and optionally search bookmarks, but cannot search the remote news catalogue.
- Bookmarks are durable, independent of feed-cache maintenance, and contain enough metadata to render article detail offline.
- One native article-detail destination serves both online and offline states.
  - Read original opens the article in the app native WebView. Open in browser opens the system default external browser. Share uses the Android share sheet.
- Connectivity status is advisory. Actual request or open failures are authoritative.

## Technology baseline

Use Kotlin, Jetpack Compose, Paging 3, Room, Retrofit, OkHttp, Hilt, Coroutines, and Flow. Use Coil for image URL memory/disk caching; store image URLs, never image blobs.

The default architecture is pragmatic MVVM with clean boundaries and focused use cases. Do not introduce excessive modularization for its own sake.

## Module boundaries

The recommended Gradle modules are:

```text
:app
:core
:data
:feature-news
```

- `:app`: Application, MainActivity, Hilt bootstrap, root navigation, theme wiring.
- `:core`: domain models, domain errors, shared UI, utilities, dispatcher abstractions, and common test helpers.
- `:data`: Retrofit/OkHttp, Room, DTOs, entities, DAOs, repositories, mappers, `RemoteMediator`, and search `PagingSource`.
- `:feature-news`: feed, search, bookmarks, article detail, ViewModels, UI state/events, and feature navigation.

The feature module may be organized internally as `feed/`, `search/`, `bookmarks/`, `detail/`, and `common/`.

## Layering rules

- Retrofit DTOs and Room entities must not reach Compose or ViewModels.
- Map data through appropriate boundaries: DTO → entity/domain → UI model as needed.
- Presentation depends on domain models and repository contracts, not Retrofit, Room, or provider-specific implementation details.
- Bookmark state is derived from the bookmark data source; never trust a copied Boolean from a network response.
- Use stable article identifiers. Prefer a hash of a normalized canonical URL when the provider has no reliable ID.
- Batch page writes and use indexed fields for ordering, uniqueness, and lookup.
- Map transport, HTTP, parsing, rate-limit, authentication, quota, validation, and unknown failures into domain-level error categories. Never expose raw exceptions or provider-specific error text directly in UI.

## Persistence contract

The database has three logical responsibilities:

### `cached_articles`

Temporary active-feed metadata and ordering. It contains article metadata, `feedPosition`, and `cachedAt`. It is populated by `RemoteMediator`, grows during active pagination, is transactionally replaced after successful refresh, and is trimmed at startup.

### `bookmarked_articles`

Self-contained durable article metadata, including `bookmarkedAt`. It persists until explicit unbookmark and is never removed by feed refresh or cache cleanup. Metadata duplication is intentional to guarantee offline detail independence.

### `feed_metadata`

A single-row record for the active request and pagination state: request key, next page/cursor, end-of-pagination state, and last successful refresh time. Changing feed parameters creates a new request and must not destroy the prior cache until the new request succeeds.

## Paging and refresh contract

- `REFRESH`: fetch the first page; on success, transactionally clear/replace `cached_articles` and reset `feed_metadata`.
- `APPEND`: read the next page/cursor from `feed_metadata`, batch insert results, and update metadata in one appropriate transaction.
- `PREPEND`: normally unsupported for a top-down current-news feed; report end of pagination.
- Empty pages and end-of-pagination must be handled explicitly.
- Refresh and append loading/errors must be distinct in the UI.
- Keep stale content visible during refresh whenever possible.

## UX and accessibility baseline

The primary navigation is Home, Search, and Bookmarks. The UI must provide loading, empty, error, retry, refresh, and placeholder states; stable `LazyColumn` keys; dark theme support; font scaling; content descriptions; usable touch targets; and sensible transitions. Deep links, adaptive layouts, and additional polish are secondary to the core flows.

## Explicit non-goals

Do not prioritize custom editorial or machine-learning ranking, permanent caches for every filter/query, offline webpage scraping or HTML/media storage, account bookmark synchronization, generation-based/double-buffer caching, a production backend proxy unless API-key restrictions require it, background notifications, or tablet two-pane layouts before the core acceptance criteria are complete.

## Decision protocol

Before changing a governed rule, document:

1. The current rule and proposed change.
2. The product or technical reason.
3. Impact on cache semantics, offline guarantees, API behavior, navigation, tests, and delivery scope.
4. Migration and rollback implications.
5. The updated acceptance criteria.

Changes to cache replacement/retention, search semantics, bookmark durability, offline detail behavior, or layer boundaries require explicit product/architecture approval. Do not make such changes as incidental refactors.

Changing the selected provider, endpoint semantics, default country/language, or provider-driven ranking behavior also requires an explicit architecture decision because it affects API contracts, pagination, quotas, caching, and acceptance tests.

## Implementation order

Detailed design and implementation should proceed in this order:

1. Validate GNews endpoint behavior, quotas, filters, search, pagination, content completeness, and API-key handling.
2. Define domain models and repository contracts.
3. Define Retrofit contracts, DTOs, mappings, Room entities, DAOs, indexes, and schema migration strategy.
4. Implement the home `Pager` and `RemoteMediator` refresh/append behavior.
5. Implement bookmarks and cross-screen bookmark-state propagation.
6. Implement debounced remote search and its query-specific `Pager`.
7. Implement Compose screens, navigation, deep links, article actions, and load-state UX.
8. Add tests for paging/cache behavior, repositories, ViewModels, and critical UI/navigation flows.
9. Harden process restart, offline cases, cache maintenance, dark mode, accessibility, API-key safety, and README documentation.

## Definition of done

The core release is complete only when scoped ranked home news paginates reliably; cached home data restores after restart and works offline; refresh replacement is atomic and failure-safe; startup trimming is bounded; search is debounced, cancellable, paginated, and state-complete; bookmarks persist independently and open native detail offline; article actions handle failures; accessibility and dark mode are verified; core repository/ViewModel/paging behavior is automated-tested; and setup, API-key configuration, architecture, trade-offs, offline behavior, and known limitations are documented.

## Source of truth

This governance file consolidates the Android News App Development Handoff and the detailed architecture/product baseline supplied with the project. If later documents conflict with this file, resolve the conflict through the decision protocol above and update this file or add a clearly linked decision record.

## Implementation status tracking

PROJECT_STATUS.md is the canonical phase checklist. Update it whenever a phase starts, completes, fails verification, or changes scope. Do not mark a phase complete until its documented completion gate passes.
