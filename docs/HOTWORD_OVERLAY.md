# Hotword + Overlay (Priority 2)

This document describes the hotword-listening overlay service and Accessibility scaffold added in the feature/hotword-overlay branch.

## Files added
- app/src/main/java/com/shahrafuking/kingassistant/overlay/OverlayService.kt
- app/src/main/java/com/shahrafuking/kingassistant/hotword/HotwordManager.kt
- app/src/main/java/com/shahrafuking/kingassistant/accessibility/AutoClickService.kt
- app/src/main/res/xml/accessibility_service_config.xml

## How to test (quick smoke test)
1. Build and install the debug APK on an Android device with microphone.
2. Open the app and grant Microphone permission.
3. Start the OverlayService (MainActivity -> Start). Service posts a foreground notification "Listening for hotword".
4. Make a loud sound (clap) to exceed the RMS threshold (placeholder detector). The service will bring the MainActivity to front when a detection occurs.

## Porcupine integration
- This scaffold includes a PORCUPINE_ENABLED flag in HotwordManager. To enable Porcupine-based detection, add the Porcupine native libs and .ppn file in assets and implement the Porcupine glue in HotwordManager.

## Accessibility & auto-tap
- AutoClickService is an AccessibilityService scaffold that can dispatch gestures. Keep this disabled until explicit opt-in and policy review. It is a powerful capability and may lead to unintended clicks or account bans in trading apps.

## Security & policy notes
- Do NOT enable automated trading or auto-click production flows without legal and policy review. Use the Accessibility scaffold only for benign automation that users explicitly allow.
