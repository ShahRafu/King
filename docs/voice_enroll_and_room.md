# Voice enrollment & Room DB

এই আপডেটে আমি নিম্নলিখিত কাজগুলো যোগ করেছি (branch: feature/voice-enrollment):

1) Room DB (local) - entities, DAOs ও AppDatabase
   - MemoryEntity, MemoryDao
   - PendingActionEntity, PendingActionDao
   - VoiceProfileEntity, VoiceProfileDao
   - AppDatabase + RoomRepository (RoomRepository provides convenience wrapper)

2) Voice Enrollment scaffold
   - VoiceEnrollmentManager: records short PCM samples (AudioRecord), computes SHA-256 over bytes as a placeholder embedding, and saves VoiceProfileEntity into DB.
   - VoiceEnrollScreen: simple Jetpack Compose UI to record samples and enroll profile.

3) build.gradle updated to include Room + kapt + coroutines dependencies.

Safety & Notes:
- The voice embedding implemented here is a placeholder (SHA-256 over bytes) and NOT secure biometric embedding. Replace with real audio-embedding model for production.
- Recorded sample files are stored in app's internal filesDir/voice_samples (not world-readable).
- DB is plain Room DB. For higher security, consider encrypting DB (e.g., SQLCipher or EncryptedRoom).

Usage:
- Checkout branch: feature/voice-enrollment
- Open project in Android Studio, run Gradle sync.
- Add navigation to VoiceEnrollScreen from your UI (Settings or Home) to enroll voice.

Next steps I can do on request:
- Add Room DAOs usage into DialogueManager (persist pending actions & memories).
- Add verification flow (VoiceAuthStub to verify against enrolled profile) using Manager.verifyVoice(...).
- Add DB encryption guidance and integrate SQLCipher.
