# আর্কিটেকচার (সংক্ষেপ)

Modules:
- ui/ : Compose UI (HomeScreen, SettingsDrawer, Conversation)
- core/ : RobotEngine, DialogueManager, IntentParser (বড় বাংলা লজিক এখানে ইনজেক্ট হবে)
- model/ : ModelClient (HTTP wrapper) — placeholder
- security/ : VoiceAuthStub (phrase detect scaffold), KeyStore/Encrypted prefs scaffold
- storage/ : LocalStore (EncryptedSharedPreferences)
- background/ : ForegroundService, BgWorker scaffold
- accessibility/ : AccessibilityStub (hook only)

Safety:
- Sensitive / potentially policy-violating functionality (IP rotation, auto‑click exploit) NOT implemented. Only hooks/stubs provided with warnings.
