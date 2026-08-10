*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/core/CommandHandler.kt
@@
-import kotlinx.coroutines.Dispatchers
-import kotlinx.coroutines.withContext
+import kotlinx.coroutines.Dispatchers
+import kotlinx.coroutines.withContext
+import com.shahrafuking.kingassistant.plugin.voiceplugin.ProductionEmbedderAdapter
@@
-            // 1) Panic detection (Bengali forms + English)
+            // 1) Panic detection (Bengali forms + English)
             val panicTriggers = listOf("সব ট্রেড বন্ধ", "সব ট্রেড বন্ধ করো", "সব ট্রেড বন্ধ কর", "সব ট্রেড বন্ধ করো!", "সব ট্রেড থামাও", "panic", "stop all trades", "stop trades", "সব বন্ধ করো", "ঘুমিয়ে যাও", "ঘুমিয়ে যাও")
             for (p in panicTriggers) {
                 if (lowered.contains(p)) {
                     // broadcast panic
                     try {
@@
                     return@withContext "Panic command received — stopping all activities now."
                 }
             }
+
+            // 1.a) Panic release via voice‑verified phrase
+            val releaseTriggers = listOf("এখন নরমাল হয়ে যাও কিং এসিস্টেন্ট", "এখন নরমাল হয়ে যাও কিং এসিস্ট্যান্ট", "এখন নরমাল হয়ে যাও কিং", "নরমাল হয়ে যাও কিং এসিস্টেন্ট")
+            for (r in releaseTriggers) {
+                if (lowered.contains(r)) {
+                    // Only attempt release if panic is currently engaged
+                    try {
+                        if (!com.shahrafuking.kingassistant.core.PanicManager.isEngaged(context)) {
+                            return@withContext "Panic not engaged. Nothing to release."
+                        }
+
+                        // Perform voice verification by recording a short sample and verifying against template
+                        // Use ProductionEmbedderAdapter which records PCM and delegates to the plugin/embedder if available
+                        val verifierAdapter = ProductionEmbedderAdapter(context)
+                        val verified = try {
+                            // record ~1.8s and verify
+                            runCatching { kotlinx.coroutines.runBlocking { verifierAdapter.verify(1800) } }.getOrNull() ?: false
+                        } catch (t: Throwable) {
+                            false
+                        }
+
+                        if (verified) {
+                            com.shahrafuking.kingassistant.core.PanicManager.release(context)
+                            return@withContext "ভয়েস ভেরিফিকেশন সফল — Panic released."
+                        } else {
+                            return@withContext "ভুল ভোকাল বা ভেরিফাই ব্যর্থ — Panic এখনও সক্রিয়।"
+                        }
+                    } catch (t: Throwable) {
+                        Log.w(TAG, "release verification error", t)
+                        return@withContext "Release verification ব্যর্থ হয়েছে।"
+                    }
+                }
+            }
*** End Patch
