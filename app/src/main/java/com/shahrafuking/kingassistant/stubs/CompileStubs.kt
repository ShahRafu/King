package com.shahrafuking.kingassistant.stubs

import android.content.Context

object PanicManager {
    fun engage(context: Context) {}
    fun release(context: Context) {}
    fun isEngaged(context: Context): Boolean = false
}

object BudgetManager {
    fun clearBudget(context: Context) {}
    fun applyBudgetCap(context: Context, amount: Double?): Double? = amount
    class BudgetManager(private val ctx: Context) {
        fun checkAndReserve(amount: Double): Boolean = true
        fun setBudget(amount: Double) {}
    }
}

class RoomRepository(context: Context) {}
