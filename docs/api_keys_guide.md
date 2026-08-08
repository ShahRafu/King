# API Keys Guide (বাংলা)

- কোন কীগুলো দরকার হবে:
  - LLM API Key (যদি remote inference ব্যবহার করেন)
  - TTS/STT API Keys (যদি আপনি উচ্চমানের voice features চান)
  - Optional: Cloud storage (S3/Firebase) credentials

- কোথায় সংরক্ষণ করবেন:
  - কখনও কোডে সরাসরি hardcode করবেন না।
  - লোকালি EncryptedSharedPreferences (LocalStore) অথবা Android Keystore-এ রাখুন।
