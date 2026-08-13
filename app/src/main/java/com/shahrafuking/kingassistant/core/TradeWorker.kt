name=app/src/main/java/com/shahrafuking/kingassistant/core/TradeWorker.kt url=https://github.com/ShahRafu/King/blob/main/app/src/main/java/com/shahrafuking/kingassistant/core/TradeWorker.kt
package com.shahrafuking.kingassistant.core

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result as WorkResult
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import com.shahrafuking.kingassistant.net.trade.SimulatedQuotexAdapter

class TradeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val TAG = "TradeWorker"

    override suspend fun doWork(): WorkResult {
        try {
            // Respect panic mode
            if (PanicManager.isEngaged(applicationContext)) {
                Log.w(TAG, "Skipping scheduled trade: panic engaged")
                return WorkResult.failure()
            }

            val instrument = inputData.getString("instrument") ?: "AUTO"
            val side = inputData.getString("side") ?: "BUY"
            val amount = if (inputData.keyValueMap.containsKey("amount_usd")) inputData.getDouble("amount_usd", Double.NaN) else Double.NaN
            val amountUsd = if (amount.isNaN()) null else amount

            val budgeted = try {
                BudgetManager.applyBudgetCap(applicationContext, amountUsd)
            } catch (t: Throwable) { amountUsd }

            val order = TradeOrder(instrument = instrument, side = side, amountUsd = budgeted)
            val adapter = SimulatedQuotexAdapter(applicationContext)
            val res = adapter.executeOrder(order, dryRun = true)
            Log.i(TAG, "Scheduled trade executed (dry-run): $res")
            return WorkResult.success()
        } catch (t: Throwable) {
            Log.w(TAG, "Scheduled trade failed", t)
            return WorkResult.retry()
        }
    }
}
