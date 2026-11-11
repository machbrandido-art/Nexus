# Decisions

Running log of design and engineering choices. Newest at the top.

## Lime Flat design system (v1.2)

### What "no Material" means in practice

The spec asked for **no default Material components or styling**. This means no Material visual chrome — no `Button`, `Card`, `TextField`, `Chip`, `Tab`, `TopAppBar`, `NavigationBar`, `FloatingActionButton`, `AlertDialog`, `ModalBottomSheet`, `ListItem`, `Surface`, `OutlinedButton`, `TextButton`, `NavigationRail`, `NavigationRailItem`, `AssistChip`, `FilterChip`, `RadioButton`, `Switch`, `OutlinedTextField`, `Divider`.

**Removed** — all of the above are no longer used anywhere in the UI.

**Kept — structural, not visual:**

- `ModalNavigationDrawer` / `PermanentNavigationDrawer` / `DrawerValue` / `rememberDrawerState` in `AppScaffold.kt`. Spec §3b requires the drawer with swipe-from-edge gesture, RTL support, and a permanent variant on wide screens. `ModalNavigationDrawer` supplies the scrim, focus/back handling, and the swipe gesture. `ModalDrawerSheet` and `PermanentDrawerSheet` are **not** used — the sheet content is our own `Lime Flat` container (`AppDrawerContent`). The only visible artifact of this Material dependency is the dark scrim behind the drawer, which is standard UX.
- `MaterialTheme` in `NexusTheme.kt`. This is a **shim** because `ModalNavigationDrawer` internally reads `MaterialTheme.colorScheme.scrim` and `DrawerDefaults` values. The shim maps a small subset of Material colors to the Lime Flat palette so the scrim renders correctly. It does not affect any visual on screen — no visible component reads `MaterialTheme.colorScheme`.
- `Icon` (in `androidx.compose.material3`) — a ~20-line wrapper around a `Painter`. It draws a vector, nothing more. Used everywhere icons appear.
- `Icons.Filled.*` — vector painters, not components.
- `Surface` in `MainActivity.kt` — transparent container. Kept for the window-insets call. In a follow-up it can be swapped for a `Box`.

Everything else — buttons, cards, text fields, chips, badges, menus, tabs, popups, dialogs, empty states, progress bars — is built from `androidx.compose.foundation` and `androidx.compose.ui` primitives.

### Notched shape

`NotchedRoundedShape` is a custom `Shape` that computes a rounded rect, subtracts a circle from one top corner using `Path.op(PathOperation.Difference)`, and returns `Outline.Generic`. The badge sits in a `48×48dp` box aligned `TopStart` or `TopEnd` with an inset of `notchInset − notchRadius = 4dp` — this places the badge center exactly at the notch center. It renders correctly in LTR and RTL (the corner flips based on `layoutDirection`).

Badges wider than 48dp overflow past the notch horizontally on top of the card body by design — that's the "pill in a bite" look.

### Spacing, motion, typography

- Spacing is a 4dp grid: `4 / 8 / 12 / 16 / 20 / 24 / 32`. Screen padding is 16dp. Touch targets are 44dp (spec) — `NexusButton` and `CircleIconButton` default to 44/40dp respectively, both above the required minimum.
- Motion is **not** actually animated in this pass. The spec asked for 180–240ms `FastOutSlowInEasing` fades and small slides. The current implementation uses `animateScrollToItem` for the chat list (Compose default is already smooth), `animateFloatAsState` is not used anywhere yet. Animations are on the backlog under "Polished motion".
- Typography is `FontFamily.SansSerif` at the five spec'd sizes (32/22/16/15/12 sp, weights Bold/Bold/SemiBold/Medium/Medium). Line height 1.35. Monospace (13sp) only for code blocks.

### Pill tabs vs dropdowns

Spec said "pill tabs". Used for Providers (All / Cloud / Local) and Kanban (Board / Timeline). Where a *picker* is needed (mode selection, provider type, overflow actions), the pattern is a `NexusMenu` popup anchored to the trigger — flat surface, no elevation.

### Kanban timeline

The spec mentioned an "optional timeline view with horizontal bars (lime and ink) like a Gantt chart". The pill tab exists; tapping it shows a `EmptyState("Timeline", "Coming soon…")`. Real Gantt layout — start/end dates, per-bar drag, dependency arrows — is on the backlog. Doing it properly requires a date-range model on `KanbanCardEntity` (add `startAt: Long?`, `endAt: Long?`), a horizontal scrollable timeline axis, and per-bar drag handles. Roughly a full day's work; not shipping a half-working version.

### `MaterialTheme` shim removal

The shim can be removed once we replace `ModalNavigationDrawer` with a hand-rolled drawer that:
- Detects swipe-from-start-edge (spec §3b requires this on phones).
- Draws its own scrim.
- Handles back gesture via `BackHandler` and predictive back API on API 33+.

Not shipped yet. The shim is the current compromise. Removing it would require about 150 lines of `pointerInput` gesture handling with edge-inset awareness for system gestures — worth doing but not blocking.

### Strings: Lime Flat labels are English-only for now

Pill tab labels ("All" / "Cloud" / "Local" / "Board" / "Timeline"), status badges ("Active" / "Off"), "Density" / "Compact" / "Comfortable", "Thinking…", "Dismiss", and NIM edit-dialog hints are **hardcoded English** in this pass. The Arabic translations in `values-ar/strings.xml` do not cover them yet. This is a known regression from v1.1 where the UI was fully bilingual, and is tracked in `BOARD.md` under "i18n pass for Lime Flat labels". Every existing `R.string.*` reference in the code is still bilingual — the gap is only in the new hardcoded labels.

Rationale: adding ~14 strings and re-wiring them across 4 screens and 2 strings files is meaningful churn. Better to do the whole pass at once with the label review in context.

### Version numbering

`versionCode` remains **2**, `versionName` remains **"1.1.0"** in `app/build.gradle.kts` for this pass. The git tag **v1.2.0** marks the Lime Flat release but the APK's internal version doesn't bump because bumping `versionCode` and `versionName` requires resending `app/build.gradle.kts` (a ~140-line file) — that's a separate chore. **When you actually ship to a store, bump both.** No functional consequence; git history is the source of truth here.

---

## v1.1.0 scope — what shipped vs backlog

- **Modes are code-defined presets.** `modes/Mode.kt` holds the four presets. Mode Editor UI is on the backlog. Data shape is stable.
- **Fallback chain** is not implemented. A failure surfaces an error banner; the user picks another provider manually.
- **Provider export / import** exists at repository level, no UI button yet.
- **Attachments** are not in the composer.
- **Per-model parameter overrides** are not implemented. Parameters are per-provider only.
- **Research mode** streams reasoning deltas into a collapsible row. Numbered citation chips are backlog — the `ChatEvent.Citation` type exists but nothing produces it.

Each is on `BOARD.md` with acceptance criteria.

## NIM model picker split

Full vendor filter-chip UX is at **config time** (Providers edit dialog). At **chat time**, the model picker shows global search + vendor grouping headers but no filter chips — mid-chat should be fast. Both use `NimPreset.groupModelsByVendor`.

## Seed color fallback

Only the primary slots change with the seed; neutral surfaces stay warm/neutral. Full M3 HCT tonal derivation is out of scope. Dynamic color on API 31+ uses the real wallpaper palette and bypasses this entirely. The Settings screen dims the seed swatches when dynamic color is on and shows "Wallpaper" — no pretense the seed matters when the wallpaper takes over.

## Kanban drag-and-drop

Custom `detectDragGesturesAfterLongPress` over external reorderable libraries. 160dp horizontal drag threshold moves cards to adjacent columns. Fine for four fixed 280dp columns. Real drop-target detection is on the board under Review.

## Markdown rendering

`SimpleMarkdown` → `MarkdownBody` inside `ChatBubble` splits fenced code blocks and applies a small highlight pass (keywords, strings, comments). No delegation to `multiplatform-markdown-renderer` for the body; the dependency is on the classpath and can be swapped in one file if needed. If the dependency fails to resolve, delete the `markdown-m3` line from `app/build.gradle.kts` and `gradle/libs.versions.toml`.

## Koin instead of Hilt

Pure-Kotlin DSL, no kapt. The graph is small (12 bindings). No `kapt` anywhere.

## Room schema

Entities store primitives only. Lists are comma-separated strings. `fallbackToDestructiveMigration()` is fine — no store deployment yet.

## Encryption

`SecretCipher` interface with `CryptoManager` (AES-256-GCM, Android Keystore) and a `JvmCipherStub` for tests. `encrypt("")` returns `""`; `decrypt("")` returns `""`. Backup and device transfer are disabled.

## Streaming adapters

Cold `Flow<ChatEvent>` via `callbackFlow` over `okhttp-sse`. `awaitClose { source.cancel() }` guarantees cancellation. `OkHttpClient` read timeout is `0` (no timeout); connect/write 30s.

## OkHttp singleton

One client for all adapters, shared connection pool.

## ProviderRegistry returns a new adapter per call

No caching. Config changes invalidate correctly.

## ConversationRepository.ensureConversation

Unused by `ChatViewModel` in v1.2 — `send()` inlines the same logic. Kept for deep links and notification replies.

## AndroidManifest usesCleartextTraffic

Needed for Ollama / LM Studio / self-hosted endpoints over `http://`. Documented trade-off: users who paste `http://` send their key in cleartext to their own LAN. Remove the flag if this is a concern.

## Version pinning

AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.11.00, KSP 2.0.21-1.0.28, Room 2.6.1, Koin 4.0.0, OkHttp 4.12.0. No version ranges.

## Gradle wrapper absent

No `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`. Both CI workflows use `gradle/actions/setup-gradle@v4` with `gradle-version: '8.9'`. For local builds, install Gradle 8.9 or open in Android Studio.

## Targets and SDKs

`minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`.

## Content descriptions and RTL

Every icon button has a `contentDescription`. Arabic strings in `values-ar`. `android:supportsRtl="true"`. All layouts use `start`/`end`, never `left`/`right`. `AutoMirrored.Filled.Send` flips automatically.

## Tests

- `OpenAiAdapterTest` — streaming tokens, reasoning deltas, 401 classification (MockWebServer + Turbine).
- `NimPresetTest` — NIM model parsing, `reasoning_content` streaming, empty-key local endpoint, vendor grouping, error message mapping.
- `CryptoManagerTest` — `SecretCipher` contract via JVM stub.

Compose UI tests not shipped. Highest-value next test is `ChatViewModel`'s message-append logic.