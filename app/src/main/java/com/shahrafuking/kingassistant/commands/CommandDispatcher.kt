package com.shahrafuking.kingassistant.commands

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CommandDispatcher {
    private val TAG = "CommandDispatcher"

    fun dispatch(context: Context, text: String) {
        val s = text.trim().lowercase()
        CoroutineScope(Dispatchers.IO).launch {
            when {
                s.contains("সব ট্রেড বন্ধ") || s.contains("stop all trades") -> emergencyStop(context)
                s.contains("বাজেট") -> {
                    val dollars = extractNumber(s)
                    if (dollars != null) setBudget(context, dollars)
                }
                s.contains("এখন ট্রেড") -> {
                    Log.i(TAG, "Start trading command received")
                }
                else -> Log.i(TAG, "Unknown command: $s")
            }
        }
    }

    private fun extractNumber(s: String): Double? {
        val regex = Regex("(\\d+(?:\\.\\d+)?)")
        val m = regex.find(s) ?: return null
        return m.groupValues[1].toDoubleOrNull()
    }

    private fun emergencyStop(context: Context) {
        Log.w(TAG, "EMERGENCY STOP: stopping trading and services")
        // TODO: implement cancel orders and stop services
    }

    private fun setBudget(context: Context, dollars: Double) {
        Log.i(TAG, "Set voice budget: $dollars")
        // TODO: persist budget
    }
}
