package com.shahrafuking.kingassistant.trading

import java.util.UUID

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val side: Side,
    val amount: Double,
    val meta: Map<String, String> = emptyMap()
)

enum class Side { BUY, SELL }

sealed class OrderEvent {
    data class Submitted(val order: Order) : OrderEvent()
    data class Filled(val orderId: String, val filledAmount: Double) : OrderEvent()
    data class Cancelled(val orderId: String) : OrderEvent()
    data class Failed(val orderId: String, val reason: String) : OrderEvent()
}
