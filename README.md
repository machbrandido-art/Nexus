# Nexus

A multi-provider AI chat app for Android. Bring your own API keys, switch providers and modes, keep your data local.

- **Kotlin 2.0**, **Jetpack Compose + Material 3**, MVVM, Koin, Room, DataStore
- Providers: **Anthropic**, **OpenAI**, **Google Gemini**, and **OpenAI-compatible** (OpenRouter, Groq, Together, Mistral, DeepSeek, xAI, Ollama, LM Studio, custom endpoints)
- **Encrypted API keys** (Android Keystore, AES-GCM) — never logged
- **Modes**: Chat, Work (Kanban), Code, Research — each with its own system prompt, temperature, and layout
- **Streaming** responses over SSE, with reasoning deltas surfaced in a collapsible block
- **Full RTL**, English + Arabic
- **No analytics, no telemetry, no vendor servers in the middle**

## Build

Requires JDK 17 and Android SDK 35.

```bash
gradle :app:assembleDebug