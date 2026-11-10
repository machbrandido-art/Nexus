
### BOARD.md

```markdown
# Board

Small cards, roughly an hour each. Updated as work lands.

## Backlog

- [ ] **Mode Editor UI** — add/edit custom modes (name, icon, system prompt, default model, temperature); persist in Room; move presets into a seeded table
- [ ] **Fallback chain** — provider A → B → C on `ChatEvent.Error` with a `retryable` classification; UI to order the chain in Provider settings
- [ ] **Per-model parameter overrides** — Room table `model_overrides(providerId, model, temperature, maxTokens, reasoningEffort)`; lookup in `ChatViewModel.startStreaming`
- [ ] **Provider export / import UI** — buttons that pipe `ProviderRepository.exportJson` / `importJson` through the share sheet and file picker
- [ ] **Code mode split pane** — adaptive layout: chat on the start half, a preview pane on the end half when width ≥ 840dp, tabs (Chat / Code / Preview) below that
- [ ] **HTML preview** — a `WebView` inside `AndroidView` fed from the last fenced ```html block, sandboxed with `setAllowFileAccess(false)` and a no-op `WebViewClient`
- [ ] **Research mode citations** — parse `[n]` markers from the assistant text, render numbered chips under the message; wire to `ChatEvent.Citation` when the provider supplies them (Gemini grounding, Perplexity-style APIs)
- [ ] **Attachments** — composer slot for images and files, `ChatMessage` with a `parts` list, Anthropic vision, OpenAI vision, Gemini inline data
- [ ] **Voice input** — `SpeechRecognizer` behind the composer's mic slot
- [ ] **Dynamic color** — read from DataStore inside `NexusTheme`; apply `dynamicLightColorScheme` / `dynamicDarkColorScheme` when enabled on API 31+
- [ ] **Compact density** — a `LocalDensity` provider that tightens list spacing; currently the toggle stores but doesn't apply
- [ ] **Conversation list with Kanban view** — pinned/archived status columns, per-mode columns
- [ ] **"Turn this reply into tasks"** — long-press a message → parse numbered tasks → create Kanban cards linked to the conversation

## Doing

*(nothing in flight)*

## Review

- [ ] **Kanban drag precision** — currently uses a 160dp horizontal drag threshold to move to a neighbouring column. Replace with hover-target detection during `onDrag` for accurate drop placement.
- [ ] **Streaming scroll** — `animateScrollToItem` fires on every token when the list is short; guard on `listState.canScrollForward` to avoid jitter on long responses.

## Done

- [x] Scaffold + CI (green pipeline first)
- [x] Theme, navigation shell, i18n / RTL (English + Arabic)
- [x] Room + `CryptoManager` (AES-GCM, Android Keystore)
- [x] `ProviderAdapter` interface + `OpenAiCompatibleAdapter`
- [x] Chat UI + SSE streaming + fenced code-block rendering
- [x] Anthropic + Gemini adapters
- [x] Providers screen + test connection + fetch models
- [x] Modes system (four presets, per-conversation persistence) + switcher
- [x] Work mode: Kanban board (4 columns, drag-to-move, add/delete)
- [x] Settings screen (theme mode, dynamic-color toggle, density toggle, privacy, version)
- [x] Unit tests: OpenAI-compatible streaming (MockWebServer + Turbine), `SecretCipher` contract
- [x] README, DECISIONS, LICENSE, CI, release workflow
- [x] v1.0.0 tag