package com.shahrafuking.kingassistant.net.trade

/**
 * Trade executor interface & small data models for orders and results.
 */
interface ITradeExecutor {
    suspend fun executeOrder(order: TradeOrder, dryRun: Boolean = true): TradeResult
}

/**
 * Very small trade order model used by the POC executor.
 */
data class TradeOrder(
    val instrument: String,
    val side: String,
    val amountUsd: Double? = null
)

/**
 * Simple result wrapper.
 */
sealed class TradeResult(val success: Boolean, val summary: String) {
    class Success(summary: String) : TradeResult(true, summary)
    class Failure(summary: String) : TradeResult(false, summary)
}

/**
 * Factory to pick a default executor (currently simulated).
 */
object TradeExecutorFactory {
    fun getDefault(context: android.content.Context): ITradeExecutor = SimulatedQuotexAdapter(context)
}
