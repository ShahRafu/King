package com.shahrafuking.kingassistant.trading

import kotlinx.coroutines.flow.SharedFlow

/**
 * Adapter interface for trading backends.
 * Implementations must be cancellable and emit OrderEvent updates via orderEvents.
 */
interface TradeAdapter {
    suspend fun submitOrder(order: Order): String // returns orderId
    suspend fun cancelOrder(orderId: String): Boolean
    val orderEvents: SharedFlow<OrderEvent>
}
