package com.shahrafuking.kingassistant.net.trade

data class TradeOrder(
    val instrument: String,
    val side: String,
    val amountUsd: Double? = null
)
