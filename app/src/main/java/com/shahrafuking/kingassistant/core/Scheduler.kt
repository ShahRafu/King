package com.shahrafuking.kingassistant.core

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import java.util.concurrent.TimeUnit
import com.shahrafuking.kingassistant.util.NumberParser

/**
 * Scheduler: helper to schedule simulated trades (dry-run) using WorkManager.
 * Provides a small relative-time parser used by CommandHandler to schedule orders.
 */
object Scheduler {
    private const val KEY_INSTRUMENT = "instrument"
    private const val KEY_SIDE = "side"
    private const val KEY_AMOUNT = "amount_usd"

    /**
     * Schedule a simulated trade after delayMs. Returns the WorkRequest id (String) for possible cancellation.
     */
    fun scheduleTrade(context: Context, order: TradeOrder, delayMs: Long): String {
        val input = Data.Builder()
            .putString(KEY_INSTRUMENT, order.instrument)
            .putString(KEY_SIDE, order.side)
        order.amountUsd?.let { input.putDouble(KEY_AMOUNT, it) }
        val work = OneTimeWorkRequestBuilder<TradeWorker>()
            .setInputData(input.build())
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(work)
        return work.id.toString()
    }

    fun cancelScheduled(context: Context, workId: String) {
        try {
            val uuid = java.util.UUID.fromString(workId)
            WorkManager.getInstance(context).cancelWorkById(uuid)
        } catch (t: Throwable) {
            // ignore invalid id
        }
    }

    /**
     * Parse relative time expressions like "5 মিনিট পরে" or "২ ঘণ্টা পরে" from free text and
     * return delay in milliseconds. Returns null if no relative time parsed.
     */
    fun parseRelativeTime(text: String): Long? {
        if (text.isBlank()) return null
        val norm = NumberParser.normalizeDigits(text).lowercase()

        // Try to match explicit numeric tokens followed by unit words
        val direct = Regex("([0-9]+(?:[\\.,][0-9]+)?)\\s*(মিনিট|মিন|minute|minutes|ঘন্টা|ঘণ্টা|hour|hours|hr)")
        val m = direct.find(norm)
        if (m != null) {
            val numText = m.groupValues[1]
            val unit = m.groupValues[2]
            val num = NumberParser.parseNumber(numText) ?: return null
            return when (unit) {
                "ঘণ্টা", "ঘন্টা", "hour", "hours", "hr" -> (num * 60.0 * 60.0 * 1000.0).toLong()
                else -> (num * 60.0 * 1000.0).toLong()
            }
        }

        // Fallback: find unit word and try to parse preceding token (which might be spelled out)
        val unitWords = listOf("মিনিট", "মিন", "minute", "minutes", "ঘন্টা", "ঘণ্টা", "hour", "hours", "hr")
        for (u in unitWords) {
            val idx = norm.indexOf(u)
            if (idx > 0) {
                // take up to 20 chars before the unit and try to parse a number from it
                val before = norm.substring(maxOf(0, idx - 20), idx).trim()
                // extract last token
                val token = before.split(Regex("\\s+"), -1).lastOrNull() ?: before
                val num = NumberParser.parseNumber(token)
                if (num != null) {
                    return when (u) {
                        "ঘণ্টা", "ঘন্টা", "hour", "hours", "hr" -> (num * 60.0 * 60.0 * 1000.0).toLong()
                        else -> (num * 60.0 * 1000.0).toLong()
                    }
                }
            }
        }

        return null
    }
}
