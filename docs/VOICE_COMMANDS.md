# Voice Commands, Budget & Panic-Stop (Priority 3)

This document describes the new voice command components added in the feature/voice-commands branch.

## Files added
- app/src/main/java/com/shahrafuking/kingassistant/voice/VoiceRecognizer.kt
- app/src/main/java/com/shahrafuking/kingassistant/voice/VoiceVerifier.kt
- app/src/main/java/com/shahrafuking/kingassistant/voice/VoiceCommandManager.kt
- app/src/main/java/com/shahrafuking/kingassistant/voice/Command.kt
- app/src/main/java/com/shahrafuking/kingassistant/trade/BudgetManager.kt
- app/src/main/java/com/shahrafuking/kingassistant/trading/PanicStopManager.kt

## Quick test
- Use the existing EnrollmentActivity to enroll an owner voice template.
- Use a small demo harness (call VoiceRecognizer.startListening) and pass recognized text to VoiceCommandManager.parse(text).
- For Trade commands: call BudgetManager.checkAndReserve(amount) to ensure budget enforcement.
- For PanicStop: trigger PanicStopManager.triggerPanicStop() and ensure listeners see event.

## Security notes
- Templates and budgets are stored encrypted via KeystoreHelper.
- No broker connectivity or auto-execution is implemented in this change.
