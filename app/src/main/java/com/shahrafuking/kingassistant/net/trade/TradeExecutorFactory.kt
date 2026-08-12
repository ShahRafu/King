package com.shahrafuking.kingassistant.net.trade

import android.content.Context

object TradeExecutorFactory {
    fun getDefault(context: Context): SimulatedQuotexAdapter = SimulatedQuotexAdapter(context)
}
