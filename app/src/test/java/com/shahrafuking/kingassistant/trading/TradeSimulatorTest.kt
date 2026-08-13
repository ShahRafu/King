package com.shahrafuking.kingassistant.trading

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TradeSimulatorTest {
    @Test
    fun submitOrder_emitsFilled() = runBlocking {
        val simulator = TradeSimulator()
        val order = Order(symbol = "TEST", side = Side.BUY, amount = 1.0)
        simulator.submitOrder(order)
        // wait for a fill event within a reasonable timeout
        val ev = withTimeout(3000L) {
            simulator.orderEvents.first { it is OrderEvent.Filled && (it as OrderEvent.Filled).orderId == order.id }
        }
        assertTrue(ev is OrderEvent.Filled)
    }

    @Test
    fun cancelOrder_emitsCancelled() = runBlocking {
        val simulator = TradeSimulator()
        val order = Order(symbol = "TEST2", side = Side.SELL, amount = 2.5)
        simulator.submitOrder(order)
        // cancel immediately
        simulator.cancelOrder(order.id)
        val ev = withTimeout(2000L) {
            simulator.orderEvents.first { it is OrderEvent.Cancelled && (it as OrderEvent.Cancelled).orderId == order.id }
        }
        assertTrue(ev is OrderEvent.Cancelled)
    }
}
