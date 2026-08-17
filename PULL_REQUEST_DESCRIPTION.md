# Pull request: fix/compile-errors-20260817

This PR adds compile-safe stubs and constants to resolve several Kotlin compile errors including missing imports, undefined constants and helper types. It is intended as a minimal, build-first patch — please replace TODOs with real implementations and secret values before shipping to production.

Files added:
- app/src/main/java/com/shahrafuking/kingassistant/stubs/PanicController.kt
- app/src/main/java/com/shahrafuking/kingassistant/stubs/RobotEngine.kt
- app/src/main/java/com/shahrafuking/kingassistant/model/ApiClientOpenAI.kt
- app/src/main/java/com/shahrafuking/kingassistant/model/ModelClient.kt
- app/src/main/java/com/shahrafuking/kingassistant/embedding/ProductionEmbedderAdapter.kt
- app/src/main/java/com/shahrafuking/kingassistant/biometric/VoiceBiometricPlugin.kt
- app/src/main/java/com/shahrafuking/kingassistant/lipsync/LipSyncVerifier.kt
- app/src/main/java/com/shahrafuking/kingassistant/hotword/HotwordConstants.kt
- app/src/main/java/com/shahrafuking/kingassistant/security/AuditLogger.kt
- app/src/main/java/com/shahrafuking/kingassistant/security/HighSecurityApprovalActivity.kt
- app/src/main/java/com/shahrafuking/kingassistant/security/VerificationActivity.kt

Summary:
- Adds missing constants: PORCUPINE_ENABLED, PORCUPINE_ACCESS_KEY, PORCUPINE_MODEL_PATH, HOTWORD_DEFAULT_THRESHOLD
- Adds CameraXImageCollector stub and LipSyncVerifier
- Adds ApiClientOpenAI accessor with BuildConfig / env fallback
- Adds ModelClient adapted to okhttp3 modern APIs
- Adds PanicController & RobotEngine with proper coroutine imports
- Adds AuditLogger and verification helpers

Testing:
- Run `./gradlew assembleDebug` locally or on CI. Replace any TODOs and supply API keys via BuildConfig or environment variables.

Security:
- Do NOT commit production API keys. Use build-time injection or secret management for OPENAI_API_KEY and PORCUPINE_ACCESS_KEY.

