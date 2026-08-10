package com.shahrafuking.kingassistant.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.shahrafuking.kingassistant.overlay.OverlayService
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shahrafuking.kingassistant.plugin.voiceplugin.ProductionEmbedderAdapter
import com.shahrafuking.kingassistant.core.Scheduler
import com.shahrafuking.kingassistant.util.NumberParser

object CommandHandler {
    private const val TAG = "CommandHandler"

    /**
     * Handle a single incoming command text (from voice or typed). Returns a short user-facing string.
     */
    suspend fun handle(context: Context, text: String): String = withContext(Dispatchers.IO) {
        val lowered = text.lowercase()

        // 1) Panic detection (Bengali forms + English)
        val panicTriggers = listOf("সব ট্রেড বন্ধ", "সব ট্রেড বন্ধ করো", "সব ট্রেড থামাও", "panic", "stop all trades", "stop trades", "সব বন্ধ করো")
        for (p in panicTriggers) {
            if (lowered.contains(p)) {
                // broadcast panic and set flag
                try {
                    PanicManager.engage(context)
                } catch (t: Throwable) {
                    Log.w(TAG, "failed to engage panic", t)
                }
                return@withContext "Panic command received — stopping all activities now."
            }
        }

        // 1.a) Panic release via voice‑verified phrase
        val releaseTriggers = listOf("এখন নরমাল হয়ে যাও কিং এসিস্টেন্ট", "এখন নরমাল হয়ে যাও কিং", "নরমাল হয়ে যাও কিং এসিস্টেন্ট")
        for (r in releaseTriggers) {
            if (lowered.contains(r)) {
                // Only attempt release if panic is currently engaged
                try {
                    if (!PanicManager.isEngaged(context)) {
                        return@withContext "Panic not engaged. Nothing to release."
                    }

                    // Perform voice verification by recording a short sample and verifying against template
                    // Use ProductionEmbedderAdapter which records PCM and delegates to the plugin/embedder if available
                    val verifierAdapter = ProductionEmbedderAdapter(context)
                    val verified = try {
                        // record ~1.8s and verify
                        runCatching { kotlinx.coroutines.runBlocking { verifierAdapter.verify(1800) } }.getOrNull() ?: false
                    } catch (t: Throwable) {
                        false
                    }

                    if (verified) {
                        PanicManager.release(context)
                        return@withContext "ভয়েস ভেরিফিকেশন সফল — Panic released."
                    } else {
                        return@withContext "ভুল ভোকাল বা ভেরিফাই ব্যর্থ — Panic এখনও সক্রিয়।"
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "release verification error", t)
                    return@withContext "Release verification ব্যর্থ হয়েছে।"
                }
            }
        }

        // 1.b) Budget set / clear detection
        val budgetSetTriggers = listOf("বাজেট", "budget", "limit", "বাজেট সেট", "set budget")
        val budgetClearTriggers = listOf("বাজেট মুছে", "clear budget", "বাজেট কেটে দাও", "বাজেট ক্লিয়ার", "বাজেট মুছো")
        for (tTrigger in budgetClearTriggers) {
            if (lowered.contains(tTrigger)) {
                try {
                    BudgetManager.clearBudget(context)
                } catch (t: Throwable) { Log.w(TAG, "clear budget failed", t) }
                return@withContext "বাজেট মুছে ফেলা হয়েছে।"
            }
        }

        for (bTrigger in budgetSetTriggers) {
            if (lowered.contains(bTrigger)) {
                // try to extract numeric amount robustly
                val budget: Double? = NumberParser.parseNumber(text)
                if (budget != null) {
                    try { BudgetManager.setBudget(context, budget) } catch (t: Throwable) { Log.w(TAG, "set budget failed", t) }
                    return@withContext "বাজেট সেট করা হয়েছে ${budget} ডলারে।"
                } else {
                    return@withContext "বাজেট সনাক্ত করা যায়নি — দয়া করে একটি নম্বর বলুন (যেমন: '২০ ডলার')."
                }
            }
        }

        // 2) Trade detection (simple heuristic)
        val looksLikeTrade = lowered.contains("ট্রেড") || lowered.contains("trade") || lowered.contains("buy") || lowered.contains("sell") || lowered.contains("বাই") || lowered.contains("সেল")
        if (looksLikeTrade) {
            // try to extract budget/amount using NumberParser
            var budget: Double? = NumberParser.parseNumber(text)

            // Enforce budget cap if set
            try {
                budget = BudgetManager.applyBudgetCap(context, budget)
            } catch (_: Throwable) {}

            val order = TradeOrder(
                instrument = "AUTO", // instrument resolution not implemented in this POC
                side = if (lowered.contains("sell") || lowered.contains("সেল")) "SELL" else "BUY",
                amountUsd = budget
            )

            // Check for scheduling keywords (e.g., "5 মিনিট পরে") and schedule if present
            val delayMs = Scheduler.parseRelativeTime(text)
            if (delayMs != null) {
                try {
                    val workId = Scheduler.scheduleTrade(context, order, delayMs)
                    return@withContext "Scheduled simulated trade in ${delayMs / 60000} minutes (id=$workId)."
                } catch (t: Throwable) {
                    Log.w(TAG, "scheduling failed", t)
                }
            }

            // Enqueue to executor immediately (dry-run)
            val executor = com.shahrafuking.kingassistant.net.trade.TradeExecutorFactory.getDefault(context)
            val result = executor.executeOrder(order, dryRun = true)
            return@withContext "Simulated trade executed: ${result}"
        }

        // Default fallback
        return@withContext "আদেশ গ্রহণ করা হয়নি — অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
    }
}
