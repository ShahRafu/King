# Assets folder for King Assistant

Place your model and porcupine files here. Do NOT commit sensitive keys in this directory; only model binaries (.tflite/.ppn) may be added as needed.

Recommended paths and filenames:

- Speaker verification model (TFLite):
  app/src/main/assets/models/speaker_model.tflite

- Anti‑spoof model (TFLite):
  app/src/main/assets/models/antispoof_model.tflite

- Porcupine keyword file (.ppn):
  app/src/main/assets/porcupine/king_keyword.ppn

Notes:
- If you use Porcupine, follow README-PHOTO-HOTWORD.md to add the dependency and set PORCUPINE_ENABLED flag.
- Do not commit API keys or access tokens into the repo. Keep them in local.properties or CI secrets.
