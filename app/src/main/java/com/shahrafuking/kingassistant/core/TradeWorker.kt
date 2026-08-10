package com.shahrafuking.kingassistant.core

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import com.shahrafuking.kingassistant.net.trade.SimulatedQuotexAdapter

class TradeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val TAG = "TradeWorker"

    override suspend fun doWork(): Result {
        try {
            // Respect panic mode
            if (PanicManager.isEngaged(applicationContext)) {
                Log.w(TAG, "Skipping scheduled trade: panic engaged")
                return Result.failure()
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
            return Result.success()
        } catch (t: Throwable) {
            Log.w(TAG, "Scheduled trade failed", t)
            return Result.retry()
        }
    }
}
