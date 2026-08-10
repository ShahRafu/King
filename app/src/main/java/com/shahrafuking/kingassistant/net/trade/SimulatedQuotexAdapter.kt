package com.shahrafuking.kingassistant.net.trade

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.memory.MemoryDatabase
import com.shahrafuking.kingassistant.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SimulatedQuotexAdapter
 * - A safe dry‑run adapter that logs the requested order instead of placing any real network calls.
 */
class SimulatedQuotexAdapter(private val context: Context) : ITradeExecutor {
    private val TAG = "SimulatedQuotex"

    override suspend fun executeOrder(order: TradeOrder, dryRun: Boolean): TradeResult = withContext(Dispatchers.IO) {
        try {
            val summary = StringBuilder()
            summary.append(if (dryRun) "[DRY-RUN] " else "[LIVE] ")
            summary.append("Instrument=${order.instrument}")
            summary.append(", Side=${order.side}")
            summary.append(", AmountUsd=${order.amountUsd ?: "unspecified"}")

            Log.i(TAG, "Simulated order: $summary")

            // Record into memory DB for auditing
            try {
                val repo = MemoryRepository(MemoryDatabase.getInstance(context).memoryDao())
                repo.addMemory(
                    text = "simulated_trade",
                    embedding = null,
                    metadata = mapOf(
                        "summary" to summary.toString(),
                        "dry_run" to dryRun.toString(),
                        "ts" to System.currentTimeMillis().toString()
                    )
                )
            } catch (t: Throwable) {
                Log.w(TAG, "failed to write simulated trade to memory db", t)
            }

            return@withContext TradeResult.Success(summary.toString())
        } catch (t: Throwable) {
            Log.w(TAG, "simulated executor error", t)
            return@withContext TradeResult.Failure("simulated executor error: ${t.message}")
        }
    }
}
