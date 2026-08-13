package com.shahrafuking.kingassistant.trading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * TradingService coordinates a TradeAdapter and a simple BudgetController testable interface.
 * - Reserves budget before submitting an order
 * - Listens for adapter OrderEvent updates and finalizes or refunds budget accordingly
 *
 * NOTE: This file uses a lightweight BudgetController interface so integration tests can
 * supply an in-memory test double without depending on AndroidKeyStore or instrumentation.
 */
class TradingService(
    private val adapter: TradeAdapter,
    private val budgetController: BudgetController,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    interface BudgetController {
        suspend fun checkAndReserve(amount: Double): Boolean
        suspend fun finalize(amount: Double)
        suspend fun refund(amount: Double)
        suspend fun getBalance(): Double
    }

    private val orders = ConcurrentHashMap<String, Order>()
    private val scope = scope

    init {
        // Single collector that reacts to adapter events and coordinates budget
        scope.launch {
            adapter.orderEvents.collect { ev ->
                when (ev) {
                    is OrderEvent.Filled -> {
                        val order = orders.remove(ev.orderId)
                        if (order != null) {
                            // finalize reserved amount
                            try {
                                budgetController.finalize(ev.filledAmount)
                            } catch (_: Throwable) {
                                // swallow to avoid crashing the collector; tests can observe inconsistencies
                            }
                        }
                    }
                    is OrderEvent.Cancelled -> {
                        val order = orders.remove(ev.orderId)
                        if (order != null) {
                            try {
                                budgetController.refund(order.amount)
                            } catch (_: Throwable) {
                            }
                        }
                    }
                    is OrderEvent.Failed -> {
                        val order = orders.remove(ev.orderId)
                        if (order != null) {
                            try {
                                budgetController.refund(order.amount)
                            } catch (_: Throwable) {
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    suspend fun submitOrder(order: Order): String {
        val reserved = budgetController.checkAndReserve(order.amount)
        if (!reserved) throw IllegalStateException("insufficient funds to reserve for order")
        val id = adapter.submitOrder(order)
        orders[id] = order
        return id
    }

    suspend fun cancelOrder(orderId: String): Boolean {
        return adapter.cancelOrder(orderId)
    }
}
