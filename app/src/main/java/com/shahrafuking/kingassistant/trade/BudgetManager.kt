package com.shahrafuking.kingassistant.trade

import android.content.Context
import com.shahrafuking.kingassistant.security.KeystoreHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * BudgetManager: stores and enforces a simple single-value remaining budget.
 * The value is stored encrypted via KeystoreHelper under the provided key.
 */
class BudgetManager(private val context: Context) {
    private val KEY = "king_budget_v1"
    private val mutex = Mutex()

    suspend fun setBudget(amount: Double) {
        mutex.withLock {
            KeystoreHelper.encryptAndStoreString(context, KEY, amount.toString())
        }
    }

    suspend fun getBudget(): Double {
        return mutex.withLock {
            val s = KeystoreHelper.decryptString(context, KEY) ?: "0"
            s.toDoubleOrNull() ?: 0.0
        }
    }

    /**
     * Atomically check remaining budget and reserve amount if available.
     * Returns true if reserved (budget decreased), false if insufficient funds.
     */
    suspend fun checkAndReserve(amount: Double): Boolean {
        return mutex.withLock {
            val cur = getBudget()
            if (amount <= 0) return@withLock false
            if (cur >= amount) {
                val remaining = cur - amount
                KeystoreHelper.encryptAndStoreString(context, KEY, remaining.toString())
                true
            } else {
                false
            }
        }
    }

    suspend fun clearBudget() {
        mutex.withLock {
            KeystoreHelper.clearStoredValue(context, KEY)
        }
    }
}
