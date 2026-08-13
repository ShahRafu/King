# Pull request template

- Summary: Add EnrollmentActivity, KeystoreHelper, SecurityUtils — voice enrollment POC and secure local template storage.
- Testing: Manual device run required for microphone enrollment. No network permissions required.
- Security: Uses AndroidKeyStore AES/GCM and stores encrypted blob in SharedPreferences (device-local). Do NOT commit production secrets.
