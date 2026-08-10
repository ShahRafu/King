*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/net/trade/SimulatedQuotexAdapter.kt
@@
     override suspend fun executeOrder(order: TradeOrder, dryRun: Boolean): TradeResult = withContext(Dispatchers.IO) {
         try {
-            val summary = StringBuilder()
+            // Check panic mode
+            try {
+                if (com.shahrafuking.kingassistant.core.PanicManager.isEngaged(context)) {
+                    val msg = "Refused: panic mode engaged"
+                    Log.w(TAG, msg)
+                    return@withContext TradeResult.Failure(msg)
+                }
+            } catch (_: Throwable) {}
+
+            // Apply budget cap if configured
+            var finalAmount = order.amountUsd
+            try {
+                finalAmount = com.shahrafuking.kingassistant.core.BudgetManager.applyBudgetCap(context, finalAmount)
+            } catch (_: Throwable) {}
+
+            val summary = StringBuilder()
             summary.append(if (dryRun) "[DRY-RUN] " else "[LIVE] ")
             summary.append("Instrument=${order.instrument}")
             summary.append(", Side=${order.side}")
-            summary.append(", AmountUsd=${order.amountUsd ?: "unspecified"}")
+            summary.append(", AmountUsd=${finalAmount ?: "unspecified"}")
 
             Log.i(TAG, "Simulated order: $summary")
*** End Patch
