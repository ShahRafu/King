package com.shahrafuking.kingassistant.core

import android.content.Context
import kotlin.math.min

/**
 * Minimal BudgetManager shim to satisfy compile-time usages.
 * Stores an optional budget cap in SharedPreferences.
 */
object BudgetManager {
    private const val PREF = "king_budget"
    private const val KEY_CAP = "budget_cap" // stored as raw long from Double.doubleToRawLongBits

    fun setBudget(context: Context, dollars: Double) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_CAP, java.lang.Double.doubleToRawLongBits(dollars)).apply()
    }

    fun clearBudget(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CAP).apply()
    }

    fun applyBudgetCap(context: Context, requested: Double?): Double? {
        if (requested == null) return null
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = prefs.getLong(KEY_CAP, Long.MIN_VALUE)
        if (raw == Long.MIN_VALUE) return requested
        val cap = java.lang.Double.longBitsToDouble(raw)
        return min(requested, cap)
    }
}
