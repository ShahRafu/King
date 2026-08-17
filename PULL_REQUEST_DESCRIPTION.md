Title: fix: compile errors — add stubs, imports, and missing constants

Summary: Adds compile-safe stubs and constants to resolve Kotlin compile errors (missing imports, undefined constants, and helper types). Minimal, build-first patch — replace TODOs and inject secrets before production.

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

Testing: Run ./gradlew assembleDebug and iterate on any remaining compile errors.

Security: Do NOT commit production API keys. Use build-time injection or secret management for OPENAI_API_KEY and PORCUPINE_ACCESS_KEY.
