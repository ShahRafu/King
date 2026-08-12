package com.shahrafuking.kingassistant.net.trade

import android.content.Context
import android.util.Log

class SimulatedQuotexAdapter(private val context: Context) {
    fun executeOrder(order: TradeOrder, dryRun: Boolean = true): String {
        // Minimal dry-run simulation string
        Log.i("SimulatedQuotexAdapter", "executeOrder dryRun=$dryRun order=$order")
        return "simulated(order=${order.instrument},side=${order.side},amount=${order.amountUsd},dryRun=$dryRun)"
    }
}
