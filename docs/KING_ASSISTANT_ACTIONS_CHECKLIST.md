King Assistant — Tasks & Implementation Checklist

Implemented / Present in repo:
- VoiceProcessor (audio fingerprint scaffold)
- VoiceEnrollmentManager (enroll/verify scaffold)
- Room DB: VoiceProfileEntity, VoiceProfileDao, AppDatabase, RoomRepository
- UI: VoiceOrb, HomeScreen
- app/build.gradle (updated)
- .github/workflows/apk-build-and-release.yml

Scaffold added (please paste these files into repo from /app/src/...):
- ApiClientOpenAI.kt
- SpeechRecognizerHelper.kt
- TextToSpeechHelper.kt
- VoiceMonitorService.kt
- HotwordDetector.kt
- CommandDispatcher.kt
- AutoClickService.kt (Accessibility stub)
- PluginManager.kt
- SecurityUtils.kt

Missing / Requires external infra:
- gradle/wrapper/gradle-wrapper.jar (upload or generate locally)
- LLM API Key (OpenAI or Hugging Face)
- STT high-quality (Whisper/AssemblyAI) — key/model
- VPN / rotating proxy provider (paid) for IP rotation
- Broker API access or legal approval for UI automation
- Vector DB / embeddings infra for lifelong memory (Pinecone/Milvus or self-host)
