# Local Self-Healing & Self-Enhancement Architecture

This document describes the on-device, local-only architecture added as scaffolding to the King Assistant project to support voice-authorized, local self-modification and self-healing workflows.

Important constraints and safety
- Everything operates locally on the mobile device; there are no network calls or code downloads in the provided scaffolding.
- The scaffolding does NOT automatically compile or execute newly written native Kotlin/Java source files inside the Android app process. Android apps cannot safely compile and load arbitrary Java/Kotlin source at runtime without heavy tooling and elevated permissions. Instead this scaffold provides a gated, owner-authorized workflow to propose, stage, test, and apply targeted patches that the owner can then build locally with Gradle on-device or on a development machine.
- The included LocalCodeExecutor is a restricted JavaScript runtime (Duktape) sandboxed with no built-ins for network or file I/O. It is suitable for small plugin logic written in JavaScript that runs inside the app process.

What was added
- app/src/main/java/com/shahrafuking/kingassistant/selfheal/VoiceAuthGatekeeper.kt
- app/src/main/java/com/shahrafuking/kingassistant/selfheal/FileIsolator.kt
- app/src/main/java/com/shahrafuking/kingassistant/selfheal/SelfHealingManager.kt
- app/src/main/java/com/shahrafuking/kingassistant/selfheal/LocalCodeExecutor.kt
- docs/LOCAL_SELF_HEALING_ARCHITECTURE.md

How the flow works (high level)
1. An error or failing test is detected (manually reported or via local test runner). Call SelfHealingManager.reportError(errorText).
2. SelfHealingManager isolates the affected file via FileIsolator.identifyAndBackup(filePath).
3. SelfHealingManager generates a conservative proposed patch (placeholder text in this scaffold) and reads the proposed change to the owner via app UI or TTS.
4. The owner must confirm by voice using VoiceAuthGatekeeper.requestOwnerApproval(prompt). Only on positive voice confirmation will the scaffold write the patch into the app's local files (internal app storage area) as a staged change.
5. The owner is instructed to run a local build/test command (./gradlew assembleDebug / ./gradlew test) on-device or on a development machine. If tests pass, SelfHealingManager can optionally move the staged change into a persistent location and create a local git commit (instructions provided). The scaffold never auto-pushes or contacts external services.

Limitations and next steps
- This is scaffolding that enforces voice authorization and targeted-file isolation. Actual code generation / intelligent patch creation is intentionally a placeholder in this commit to avoid unsupervised code modification risk. You can add a local code generation module later that runs inside the device (for example, a small on-device model or deterministic template-based transform); that module must be plugged into SelfHealingManager and will only apply patches after voice approval.
- To enable Java/Kotlin runtime compilation and dynamic loading you would need to integrate an on-device compiler and dynamic class loader which is non-trivial and out of scope for a safe default scaffold.

Security notes
- Keep the Owner voice model and enrollment data local and encrypted. Use the existing Memory/SQLCipher recommendations (MEMORY.md) to protect biometric or voice embeddings.
- Voice confirmation is necessary but not sufficient for safety — consider adding a secondary PIN or device unlock check before destructive operations.

See inline docs in the Kotlin files for API-level usage examples.
