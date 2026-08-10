package com.shahrafuking.kingassistant.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * BudgetManager
 * - Stores owner's voice‑budget (USD) persistently in SharedPreferences
 * - Provides helpers to set/get/clear and to apply cap to orders
 */
object BudgetManager {
    private const val PREFS = "king_prefs"
    private const val KEY_BUDGET = "voice_budget_usd"
    private const val TAG = "BudgetManager"

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBudget(ctx: Context, amountUsd: Double) {
        try {
            prefs(ctx).edit().putString(KEY_BUDGET, amountUsd.toString()).apply()
            Log.i(TAG, "budget set: $amountUsd")
        } catch (t: Throwable) { Log.w(TAG, "setBudget error", t) }
    }

    fun getBudget(ctx: Context): Double? = try {
        val s = prefs(ctx).getString(KEY_BUDGET, null) ?: return null
        s.toDoubleOrNull()
    } catch (t: Throwable) { null }

    fun clearBudget(ctx: Context) {
        try {
            prefs(ctx).edit().remove(KEY_BUDGET).apply()
            Log.i(TAG, "budget cleared")
        } catch (t: Throwable) { Log.w(TAG, "clearBudget error", t) }
    }

    /**
     * Apply budget cap to an amount if a budget exists. Returns the capped amount.
     */
    fun applyBudgetCap(ctx: Context, requestedAmount: Double?): Double? {
        if (requestedAmount == null) return null
        val b = getBudget(ctx) ?: return requestedAmount
        return if (requestedAmount <= b) requestedAmount else b
    }
}
