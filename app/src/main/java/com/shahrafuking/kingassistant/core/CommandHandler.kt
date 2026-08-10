*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/core/CommandHandler.kt
@@
-            // 1) Panic detection (Bengali forms + English)
+            // 1) Panic detection (Bengali forms + English)
             val panicTriggers = listOf("সব ট্রেড বন্ধ", "সব ট্রেড বন্ধ করো", "সব ট্রেড বন্ধ কর", "সব ট্রেড বন্ধ করো!", "সব ট্রেড থামাও", "panic", "stop all trades", "stop trades", "সব বন্ধ করো", "ঘুমিয়ে যাও", "ঘুমিয়ে যাও")
             for (p in panicTriggers) {
                 if (lowered.contains(p)) {
                     // broadcast panic
                     try {
-                        val i = Intent(OverlayService.ACTION_PANIC_STOP)
-                        i.action = OverlayService.ACTION_PANIC_STOP
-                        context.sendBroadcast(i)
+                        // Use PanicManager to centralize behavior
+                        com.shahrafuking.kingassistant.core.PanicManager.engage(context)
                     } catch (t: Throwable) {
                         Log.w(TAG, "failed to broadcast panic", t)
                     }
 
                     return@withContext "Panic command received — stopping all activities now."
                 }
             }
+
+            // 1.b) Budget set / clear detection
+            val budgetSetTriggers = listOf("বাজেট", "budget", "limit", "বাজেট সেট", "set budget")
+            val budgetClearTriggers = listOf("বাজেট মুছে", "clear budget", "বাজেট কেটে দাও", "বাজেট ক্লিয়ার", "বাজেট মুছো")
+            for (tTrigger in budgetClearTriggers) {
+                if (lowered.contains(tTrigger)) {
+                    try {
+                        com.shahrafuking.kingassistant.core.BudgetManager.clearBudget(context)
+                    } catch (t: Throwable) { Log.w(TAG, "clear budget failed", t) }
+                    return@withContext "বাজেট মুছে ফেলা হয়েছে।"
+                }
+            }
+
+            for (bTrigger in budgetSetTriggers) {
+                if (lowered.contains(bTrigger)) {
+                    // try to extract first numeric token and set as budget
+                    val numRegex = Regex("(\\d+[\\.,]?\\d*)")
+                    val m = numRegex.find(lowered)
+                    val budget: Double? = m?.value?.replace(',', '.')?.toDoubleOrNull()
+                    if (budget != null) {
+                        try { com.shahrafuking.kingassistant.core.BudgetManager.setBudget(context, budget) } catch (t: Throwable) { Log.w(TAG, "set budget failed", t) }
+                        return@withContext "বাজেট সেট করা হয়েছে ${budget} ডলারে।"
+                    } else {
+                        return@withContext "বাজেট সনাক্ত করা যায়নি — দয়া করে একটি নম্বর বলুন (যেমন: '২০ ডলার')."
+                    }
+                }
+            }
@@
-            if (looksLikeTrade) {
+            if (looksLikeTrade) {
                 // try to extract budget (simple number extraction)
                 // supports formats like "20", "২০"(Bengali digits not fully supported here), "20.5", and optional words 'ডলার' or '$' or 'usd'
                 val numRegex = Regex("(\\d+[\\.,]?\\d*)")
                 val m = numRegex.find(lowered)
-                val budget: Double? = m?.value?.replace(',', '.')?.toDoubleOrNull()
+                var budget: Double? = m?.value?.replace(',', '.')?.toDoubleOrNull()
@@
-                val order = TradeOrder(
+                // Enforce budget cap if set
+                try {
+                    budget = com.shahrafuking.kingassistant.core.BudgetManager.applyBudgetCap(context, budget)
+                } catch (_: Throwable) {}
+
+                val order = TradeOrder(
                     instrument = "AUTO", // instrument resolution not implemented in this POC
                     side = if (lowered.contains("sell") || lowered.contains("সেল")) "SELL" else "BUY",
                     amountUsd = budget
                 )
*** End Patch
