package com.shahrafuking.kingassistant.core

import android.content.Context
import android.util.Log
import com.shahrafuking.kingassistant.net.trade.TradeOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shahrafuking.kingassistant.plugin.voiceplugin.ProductionEmbedderAdapter
import com.shahrafuking.kingassistant.core.Scheduler
import com.shahrafuking.kingassistant.util.NumberParser

object CommandHandler {
    private const val TAG = "CommandHandler"

    suspend fun handle(context: Context, text: String): String = withContext(Dispatchers.IO) {
        val lowered = text.lowercase()

        // Example numeric parse usage:
        val budget: Double? = NumberParser.parseNumber(text)

        // existing logic remains; ensure all parse calls use NumberParser.parseNumber(...)
        // (Keep the rest of CommandHandler content as-is in your repo, only ensure import and usages are corrected.)

        return@withContext "আদেশ গ্রহণ করা হয়নি — অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
    }
}
