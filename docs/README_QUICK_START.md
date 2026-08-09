King Assistant — Quick Start (MVP)

1) Add API key (for LLM):
   - Local: project root local.properties (DO NOT commit)
       API_KEY="sk_...YOUR_KEY..."
   - CI (GitHub Actions): Settings → Secrets → Actions → New repository secret
       Name: API_KEY
       Value: <your_key>

2) Ensure app/build.gradle contains:
   buildConfigField "String", "API_KEY", "\"${project.findProperty('API_KEY') ?: ""}\""

3) Upload gradle wrapper jar or accept workflow fallback.

4) Add the scaffold files (net, voice, service, docs) via GitHub UI (Create new file + paste).

5) Test on device:
   - Grant RECORD_AUDIO permission
   - Start VoiceMonitorService
   - Say: "King Assistant" → then command "সব ট্রেড বন্ধ করো" to test emergency stop flow.

Security & Legal:
- Do not hardcode API keys.
- Encrypt biometric data; use SecurityUtils and consider SQLCipher for Room.
- Review broker ToS before UI automation.
