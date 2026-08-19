## What

This PR fixes several compose-related compile errors and adds a simple DataStore-backed Settings UI for Raghu preview modes.

### Fixes
- Add missing imports for Compose Icons and material components (Icons.Filled.*).  
- Add getValue delegate import for animateFloat & state delegates.  
- Fix float/int multiplication ambiguity in VoiceOrb sizing.  
- Add SettingsDrawerContent with DataStore preferences to persist Raghu preview mode.

### Files changed
- app/src/main/java/com/shahrafuking/kingassistant/ui/screens/KingHomeScreen.kt
- app/src/main/java/com/shahrafuking/kingassistant/ui/screens/VoiceOrb.kt
- app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawerContent.kt
- app/build.gradle (add DataStore dependency)

## Why
These compile errors prevented building the debug APK on CI. The settings UI is an initial implementation for the Raghu preview appearance and can be wired into the main UI in a follow-up.

## Notes
- RF auto-submit worker not included in this PR; will add in a follow-up if desired.
