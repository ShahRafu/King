# Testing Guide

This document describes how to run the demo voice UI and unit tests added in feature/demo-voice-and-tests.

Manual demo (device)
1. Install debug APK on a real Android device.
2. Enroll with EnrollmentActivity (Main menu -> Enrollment) to store a voice template.
3. Open app and use the demo buttons on the home screen:
   - "Demo: Start voice listen" — uses device SpeechRecognizer and passes recognized text to the parser.
   - "Demo: Set budget $100" — sets an encrypted budget using KeystoreHelper.
   - "Demo: Simulate trade $10" — attempts to reserve $10 from budget.
   - "Trigger PanicStop" — triggers global panic stop event.

Unit tests
- Run from project root:
  ./gradlew test

Notes
- BudgetManager uses KeystoreHelper (AndroidKeyStore) — unit tests that exercise BudgetManager require Android/instrumentation tests and are not included in this change. The included unit tests focus on the parser logic.
