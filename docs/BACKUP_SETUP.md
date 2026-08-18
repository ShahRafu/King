# Backup setup for King Assistant

This document explains how the in-app backup system uploads backups to a GitHub private repository (used as your backup store). It also covers the required configuration in the app.

Important notes
- Backups are plain (non-encrypted) zip/text files. Keep your GitHub token secure.
- GitHub's Contents API is used to create/update files in the target repository. That API is not ideal for very large files (> ~100 MB). For large backup datasets consider using releases, LFS, or an external storage service.

How it works
- The app creates timestamped backup files (zip or text) under its internal storage (filesDir).
- A weekly WorkManager job (enabled by default) creates a backup and attempts to upload it to the configured GitHub private repo.
- Manual backups can be triggered from Settings → Media Backup → select files → Backup Selected.

Configuration steps
1) Create a GitHub Personal Access Token (PAT):
   - Go to GitHub → Settings → Developer settings → Personal access tokens
   - Create a token with `repo` scope (allows reading/writing repository contents)
   - Copy the token value — you'll paste it into the app (see step 3)

2) Create or verify your target private repository exists. Example (as used by this app):
   Owner: ShahRafu
   Repo: Rocky-Ahmed-Shsh-Rafu

3) Open the app and go to Settings → Backup GitHub Owner/Repo and enter the owner and repo name.

4) Go to Settings → Manage API Keys and add a key named `GITHUB_BACKUP_TOKEN` with your PAT value. This token is stored using EncryptedSharedPreferences.

5) Ensure Backup Enabled is ON in Settings (it is ON by default). The weekly backup job will run automatically and upload backups to the specified repo path (default: `backups/`).

Testing a manual backup
- Settings → Media Backup → pick images/files (from the app files) → "Backup Selected" will create a zip and upload it to the repo.
- Verify the `backups/` folder in your private repo contains the timestamped backup file.

Security considerations
- Treat the PAT as a secret. Do not commit it to code or share it.
- Backups are not encrypted by default. If you require encryption, add a layer (AES zip) before uploading.

Limitations
- GitHub Contents API file size limits may reject very large files. For larger backups consider a different upload/storage strategy.

