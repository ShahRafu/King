TFLite Embedder usage

1. Place converted speaker embedding model at:
   app/src/main/assets/speaker_embedder.tflite

2. Add TensorFlow Lite dependency to app/build.gradle (app module):
   implementation "org.tensorflow:tensorflow-lite:2.11.0"

3. Build the app. If the model or dependency is missing, embedder attempts will fail gracefully and code will fall back to the spectral verifier.

4. Tune DEFAULT_THRESHOLD in VoiceVerifier.kt after collecting several enroll/verify pairs on your device (itel S25).
