package com.shahrafuking.kingassistant.trading

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class TradingServiceIntegrationTest {

    @Test
    fun submitOrder_reducesBalance_onFill() = runBlocking {
        val startBalance = 100.0
        val budget = InMemoryBudgetController(startBalance)
        val simulator = TradeSimulator()
        val service = TradingService(simulator, budget)

        val order = Order(symbol = "TEST", side = Side.BUY, amount = 10.0)
        val id = service.submitOrder(order)

        // Wait for filled event
        val ev = withTimeout(3000L) {
            simulator.orderEvents.first { it is OrderEvent.Filled && (it as OrderEvent.Filled).orderId == id }
        }
        assertTrue(ev is OrderEvent.Filled)

        // Final balance should be start - amount (reserve deducted at submit)
        val bal = budget.getBalance()
        assertEquals(startBalance - order.amount, bal, 0.0001)
    }

    @Test
    fun cancelOrder_restoresBalance_onCancel() = runBlocking {
        val startBalance = 100.0
        val budget = InMemoryBudgetController(startBalance)
        val simulator = TradeSimulator()
        val service = TradingService(simulator, budget)

        val order = Order(symbol = "TEST2", side = Side.SELL, amount = 25.0)
        val id = service.submitOrder(order)

        // Cancel immediately
        val cancelled = service.cancelOrder(id)
        assertTrue(cancelled)

        // Wait for cancelled event
        val ev = withTimeout(2000L) {
            simulator.orderEvents.first { it is OrderEvent.Cancelled && (it as OrderEvent.Cancelled).orderId == id }
        }
        assertTrue(ev is OrderEvent.Cancelled)

        // Balance should be restored
        val bal = budget.getBalance()
        assertEquals(startBalance, bal, 0.0001)
    }
}
