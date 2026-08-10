package com.shahrafuking.kingassistant.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.shahrafuking.kingassistant.memory.MemoryDatabase
import com.shahrafuking.kingassistant.memory.MemoryRepository
import com.shahrafuking.kingassistant.overlay.OverlayService
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import com.shahrafuking.kingassistant.net.trade.TradeExecutorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * CommandHandler
 *
 * - Very small, safe parser for owner voice replies.
 * - Detects panic phrases and simple trade requests with optional budget.
 * - Records an audit entry into MemoryRepository for every handled command.
 *
 * NOTE: This is intentionally conservative. All execution is dry‑run by default.
 */
object CommandHandler {
    private const val TAG = "CommandHandler"

    suspend fun handle(context: Context, ownerText: String): String = withContext(Dispatchers.IO) {
        val repo = MemoryRepository(MemoryDatabase.getInstance(context).memoryDao())
        val text = ownerText.trim()
        try {
            // Record raw command into memory for audit
            try {
                repo.addMemory(
                    text = "owner_command",
                    embedding = null,
                    metadata = mapOf("command" to text, "ts" to System.currentTimeMillis().toString())
                )
            } catch (t: Throwable) {
                Log.w(TAG, "failed to write audit memory", t)
            }

            val lowered = text.lowercase(Locale.getDefault())

            // 1) Panic detection (Bengali forms + English)
            val panicTriggers = listOf("সব ট্রেড বন্ধ", "সব ট্রেড বন্ধ করো", "সব ট্রেড বন্ধ কর", "সব ট্রেড বন্ধ করো!", "সব ট্রেড থামাও", "panic", "stop all trades", "stop trades", "সব বন্ধ করো", "ঘুমিয়ে যাও", "ঘুমিয়ে যাও")
            for (p in panicTriggers) {
                if (lowered.contains(p)) {
                    // broadcast panic
                    try {
                        val i = Intent(OverlayService.ACTION_PANIC_STOP)
                        i.action = OverlayService.ACTION_PANIC_STOP
                        context.sendBroadcast(i)
                    } catch (t: Throwable) {
                        Log.w(TAG, "failed to broadcast panic", t)
                    }

                    return@withContext "Panic command received — stopping all activities now."
                }
            }

            // 2) Detect trade intent (simple heuristics)
            val looksLikeTrade = lowered.contains("ট্রেড") || lowered.contains("trade") || lowered.contains("buy") || lowered.contains("sell") || lowered.contains("বাই") || lowered.contains("সেল")
            if (looksLikeTrade) {
                // try to extract budget (simple number extraction)
                // supports formats like "20", "২০"(Bengali digits not fully supported here), "20.5", and optional words 'ডলার' or '$' or 'usd'
                val numRegex = Regex("(\\d+[\\.,]?\\d*)")
                val m = numRegex.find(lowered)
                val budget: Double? = m?.value?.replace(',', '.')?.toDoubleOrNull()

                val order = TradeOrder(
                    instrument = "AUTO", // instrument resolution not implemented in this POC
                    side = if (lowered.contains("sell") || lowered.contains("সেল")) "SELL" else "BUY",
                    amountUsd = budget
                )

                // Execute via default trade executor (dry-run by default)
                try {
                    val executor = TradeExecutorFactory.getDefault(context)
                    val result = executor.executeOrder(order, dryRun = true)

                    // record execution attempt
                    try {
                        repo.addMemory(
                            text = "trade_attempt",
                            embedding = null,
                            metadata = mapOf(
                                "order_instrument" to order.instrument,
                                "order_side" to order.side,
                                "order_amount_usd" to (order.amountUsd?.toString() ?: "null"),
                                "result" to result.summary,
                                "dry_run" to "true",
                                "ts" to System.currentTimeMillis().toString()
                            )
                        )
                    } catch (t: Throwable) { Log.w(TAG, "failed to write trade attempt to memory", t) }

                    return@withContext "Simulated trade executed: ${result.summary}"
                } catch (t: Throwable) {
                    Log.w(TAG, "trade execution failed", t)
                    return@withContext "Failed to execute simulated trade: ${t.message}"
                }
            }

            // 3) nothing actionable
            return@withContext "No actionable command detected."
        } catch (t: Throwable) {
            Log.w(TAG, "command handling error", t)
            return@withContext "Error processing command"
        }
    }
}
