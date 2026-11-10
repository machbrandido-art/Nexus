# Decisions

Running log of design and engineering choices. Newest at the top of each section.

## v1 scope — what shipped vs. what is called out as missing

I built the product end-to-end for the core loop (configure provider → pick model → chat → stream → mode-aware system prompt → kanban). A few "complete the loop" pieces are stubbed with a clear placeholder and tracked in `BOARD.md` rather than shipped half-working:

- **Modes are code-defined presets.** `modes/Mode.kt` holds the four presets. The spec calls for a Mode Editor UI; that's on the backlog, not in v1. The data shape is already the same, so migrating to a Room table later won't change call sites.
- **Dynamic color and density toggles persist but do not yet apply.** `SettingsRepository` and the Settings screen store the values; wiring them into `NexusTheme` and `LocalDensity` is on the backlog. Turning the switch on is a no-op today, and the Settings screen doesn't pretend otherwise.
- **Code mode is a monospace system prompt, not a split pane.** The four modes have distinct prompts and temperatures; the visual layout difference (split pane with `WebView`) is backlog.
- **Research mode streams reasoning deltas** (Anthropic thinking, OpenAI `reasoning_content`, DeepSeek) into a collapsible block. Numbered citation chips are backlog — the `ChatEvent.Citation` type exists in the interface but nothing produces it yet.
- **Fallback chain** is not implemented. A failure surfaces an `ErrorBanner` with retry; the user picks another provider manually.
- **Provider export/import** exists at the repository level (`exportJson`, `importJson`) but has no UI button yet.
- **Attachments** are not in the composer. The provider adapters declare `Capabilities.vision`, but the composer doesn't offer a slot.
- **Per-model parameter overrides** are not implemented. Parameters are per-provider only. This was a deliberate scope cut: it doubles the config UI for modest benefit at v1.

Each is on `BOARD.md` with acceptance criteria. Nothing else in the spec is missing without a note.

## Kanban drag-and-drop

Chose a **custom `detectDragGesturesAfterLongPress`** implementation over pulling in `Calvin-LL/Reorderable` or `sh.calvin.reorderable`.

- Pros of the custom approach: zero new dependency, no version conflicts with Compose BOM 2024.11, full control over the touch slop.
- Con: the drop-target detection uses a **160dp horizontal drag threshold** to move to the adjacent column. That's fine for the four fixed columns (280dp each) but doesn't feel as precise as hover-during-drag. A better version is on the board under Review.

Any external reorderable library would still need a custom bridge for cross-column moves, so the trade-off was not worth the dependency.

## Markdown rendering

The spec recommended `mikepenz/multiplatform-markdown-renderer-m3` and it is declared as a dependency, but the `SimpleMarkdown` composable in `ui/components/CommonComponents.kt` does its own **fenced-code-block splitting** and basic header styling rather than delegating entirely to that library.

- Reason: the library version pinned (`0.30.0`) is not guaranteed to be available under every Compose BOM release. Keeping a hand-rolled renderer that only relies on Compose means the build doesn't break if the library resolves differently.
- The `markdown-m3` dependency is still on the classpath (and ProGuard keeps it) so swapping `SimpleMarkdown`'s body to the library later is a one-file change.
- If the dependency fails to resolve during a build, delete the `markdown-m3` line from `app/build.gradle.kts` and `gradle/libs.versions.toml`; nothing else uses it.

## Koin instead of Hilt

Hilt needs annotation processing and a Gradle plugin; with KSP already in play for Room, adding Hilt's KAPT path would slow the build. Koin 4.0 is a pure-Kotlin DSL, works cleanly with KSP, and the DI graph is small (12 bindings). No `kapt` anywhere in the project.

## Room schema, no type converters

Entities store primitive columns only: `String`, `Long`, `Float?`, `Int?`, `Boolean`. Lists (models, labels) are stored as comma-separated `String` columns.

- Pros: no `TypeConverter` boilerplate, no `@Serializable` round-trip inside the DB, easier SQLite inspection.
- Con: renaming a provider type or adding a comma to a model id would corrupt the field. Neither happens in v1; a proper normalisation pass is on the backlog for `model_overrides` when that lands.

## Encryption

`SecretCipher` is a small interface with two methods (`encrypt`, `decrypt`). `CryptoManager` implements it with AES-256-GCM and a per-install key generated in `AndroidKeyStore`. IV is prepended to the ciphertext; the whole blob is Base64'd.

- Reason for the interface: JVM unit tests can't touch the Android Keystore. The unit test `CryptoManagerTest` substitutes a `JvmCipherStub` that uses plain `javax.crypto` with a random AES key and the same IV + payload layout. It verifies round-trip, non-determinism, empty-string short-circuit, and failure-on-short-ciphertext. The real `CryptoManager` is only exercised on device.
- `encrypt("")` returns `""`; `decrypt("")` returns `""`. This keeps the empty-API-key case simple.
- No `android:allowBackup="true"` anywhere: keys never leave the device via cloud backup or device transfer (both are excluded by `backup_rules.xml` and `data_extraction_rules.xml`).

## Streaming adapter shape

`ProviderAdapter.streamChat` is a **cold `Flow<ChatEvent>`** implemented with `callbackFlow` over `okhttp-sse`. Cancelling the collecting coroutine cancels the `EventSource` via `awaitClose { source.cancel() }`.

- Zero-buffer semantics: the UI receives tokens as they arrive.
- Cancellation is guaranteed on `ViewModel.onCleared` because the `streamJob` is cancelled there via `viewModelScope`. In practice the job is also cancelled whenever `send()` re-enters, and the `finally` block persists whatever text was accumulated before cancellation so the user doesn't lose output when they tap Stop.
- `OkHttpClient` read timeout is `0` (no timeout) so long streams don't get cut; connect and write timeouts are 30s. This is set once in `AppModule`.

## `OkHttpClient` singleton

A single `OkHttpClient` is shared across all adapters (`ProviderRegistry` takes it as a constructor param). Connection pooling and the dispatcher are shared, which is right for a phone app with one active stream at a time.

## `ProviderRegistry` returns a new adapter per `adapterFor()` call

Adapters are cheap (they only hold the config and a reference to the shared client). A registry that cached adapters per provider id would need to invalidate on config edits; the current shape has no stale-config bug and the allocation cost is negligible.

## `ConversationRepository.ensureConversation`

Not used by `ChatViewModel` in v1 — `send()` inlines the same logic (`if conversationId null → createConversation`). Left in the repository because it will be the entry point for deep links and notification replies later. Marked as `suspend` and idempotent so callers can race safely.

## `AndroidManifest.xml` `usesCleartextTraffic="true"`

Needed so that **Ollama, LM Studio, and self-hosted endpoints** work over `http://` on a LAN. This is a trade-off: a user who pastes an `http://` base URL will send their API key in cleartext. That's their choice and their LAN. Documented here, and the README tells users to prefer `https://`. If this is a concern, the manifest flag can be removed and only the built-in `https://` endpoints will work.

## Version pinning

Versions in `gradle/libs.versions.toml` are pinned (AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.11.00, KSP 2.0.21-1.0.28, Room 2.6.1, Koin 4.0.0, OkHttp 4.12.0). No version ranges. This is intentional: dynamic ranges make CI non-deterministic and every Android engineer has been burned by a surprise AGP bump.

## Gradle wrapper is absent

Per the project rules, no `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` binary files are checked in. Both CI workflows use `gradle/actions/setup-gradle@v4` with `gradle-version: '8.9'`, which installs Gradle at that version. For local builds, install Gradle 8.9 or open the project in Android Studio (which uses its own bundled Gradle).

## Targets and SDKs

`minSdk = 26` (Android 8.0), `compileSdk = 35`, `targetSdk = 35`. `minSdk 26` was chosen because it (a) has stable `AndroidKeyStore` AES-GCM support, (b) has `DataStore` and `Room` fully working, and (c) excludes a negligible fraction of the install base. `compileSdk 35` because it's what Android Studio Ladybug ships with and what AGP 8.7.2 targets without warnings.

## Theme

Custom warm neutral scheme in `ui/theme/Theme.kt`, not `dynamicLightColorScheme` from the OS. This is a design choice: the calm, low-contrast warm palette is the visual identity of the app. A `dynamic color` toggle exists in Settings (default off) as a placeholder for users who prefer OS-tinted theming; see "v1 scope" above for why it doesn't apply yet.

## Content descriptions and RTL

Every icon button has a `contentDescription` that resolves to a string resource. `stringResource` on an icon's `contentDescription` is not shown by TalkBack when the icon sits inside a `Button` with a text label, but it is essential for icon-only buttons (send/stop, delete, duplicate, add). Arabic strings are in `values-ar`. The manifest sets `android:supportsRtl="true"`. All layouts use `start`/`end`, never `left`/`right`. The `AutoMirrored.Filled.Send` icon flips automatically.

## Tests

Two test classes only:
- `OpenAiAdapterTest` — streaming token ordering, reasoning deltas, and 401 error classification, all against `MockWebServer`.
- `CryptoManagerTest` — the `SecretCipher` contract via a JVM stub.

No Compose UI tests in v1: Compose testing pulls in `createComposeRule` and the debug manifest, and the marginal value over the unit tests for this app's UI (mostly layout, little logic) didn't justify the CI time. A focused test on `ChatViewModel`'s message-appending logic is the highest-value next test and is on the board.