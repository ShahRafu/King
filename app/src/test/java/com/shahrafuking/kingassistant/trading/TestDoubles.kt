package com.shahrafuking.kingassistant.trading

import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory, thread-safe BudgetController test double used by unit/integration tests.
 * - startBalance initial balance
 * - checkAndReserve reduces available balance optimistically; finalize confirms consumption;
 *   refund returns reserved amount.
 */
class InMemoryBudgetController(startBalance: Double) : TradingService.BudgetController {
    private val balance = AtomicReference(startBalance)

    // For simplicity we don't track separate reserved vs available; reserve is deducted and refunded on cancel.

    override suspend fun checkAndReserve(amount: Double): Boolean {
        while (true) {
            val cur = balance.get()
            if (cur < amount) return false
            val updated = cur - amount
            if (balance.compareAndSet(cur, updated)) return true
        }
    }

    override suspend fun finalize(amount: Double) {
        // finalize is a no-op because reserve already deducted; kept for symmetry
    }

    override suspend fun refund(amount: Double) {
        while (true) {
            val cur = balance.get()
            val updated = cur + amount
            if (balance.compareAndSet(cur, updated)) return
        }
    }

    override suspend fun getBalance(): Double = balance.get()
}
