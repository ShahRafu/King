*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/overlay/OverlayService.kt
@@
-                            Log.i(TAG, "Owner replied (bn): $text")
-                            mainScope.launch {
-                                try {
-                                    if (targetLang == "bn" || targetLang.startsWith("bn") || targetLang == "und") {
-                                        Log.i(TAG, "No back-translation (target=$targetLang)")
-                                        // Optionally speak owner's reply back or log
-                                    } else {
-                                        // translate owner reply BN -> targetLang and speak
-                                        val translated = try { languageTranslator?.translateOwnerReplyAndSpeak(text, targetLang) } catch (t: Throwable) { "" }
-                                        Log.i(TAG, "Relayed owner reply -> $targetLang: $translated")
-                                    }
-                                } catch (t: Throwable) {
-                                    Log.w(TAG, "reply relay error", t)
-                                } finally {
-                                    cleanupCommandSession()
-                                }
-                            }
+                            Log.i(TAG, "Owner replied (bn): $text")
+                            mainScope.launch {
+                                try {
+                                    // Pass owner's reply into the CommandHandler which will detect panic/trade/budget
+                                    val resultMessage = com.shahrafuking.kingassistant.core.CommandHandler.handle(applicationContext, text)
+                                    // Optionally speak the brief result to owner
+                                    if (!resultMessage.isNullOrBlank()) {
+                                        languageTranslator?.speakSafely(resultMessage, "bn-BD")
+                                    }
+                                } catch (t: Throwable) {
+                                    Log.w(TAG, "reply handling error", t)
+                                } finally {
+                                    cleanupCommandSession()
+                                }
+                            }
*** End Patch
