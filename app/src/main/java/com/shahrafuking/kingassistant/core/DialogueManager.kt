package com.shahrafuking.kingassistant.core

import android.content.Context
import com.shahrafuking.kingassistant.storage.LocalStore
import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * DialogueManager
 * - Session & memory management.
 * - Decision scoring (placeholder heuristics). Heavy computations should be delegated to ModelClient or remote nodes.
 *
 * Safety: This manager will NOT execute trades itself. It will create Actions that require explicit confirmation
 * (voice + settings) before actual execution. Trade execution hook is intentionally left as a safe interface.
 */

data class Action(val id: String, val type: String, val meta: Map<String, Any?>)
data class DecisionReport(val probability: Double, val reasons: List<String>)

class DialogueManager(private val context: Context? = null) {

    private val localStore: LocalStore? = context?.let { LocalStore(it) }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // In-memory pending actions (require explicit confirmation)
    private val pendingActions = mutableMapOf<String, Action>()

    fun parseIntentToAction(intent: Intent): Action {
        return when (intent) {
            is Intent.AuthRequest -> Action(id = genId(), type = "auth_request", meta = mapOf("phrase" to intent.phrase))
            is Intent.PanicStop -> Action(id = genId(), type = "panic_stop", meta = emptyMap<String, Any?>())
            is Intent.SetBudget -> Action(id = genId(), type = "set_budget", meta = mapOf("budget" to intent.budgetUsd))
            is Intent.TradeRequest -> {
                // perform probability estimate asynchronously (placeholder)
                val report = estimateTradeProbability(intent)
                // create an action that is *pending* until explicit voice confirmation
                Action(id = genId(), type = "trade_intent", meta = mapOf(
                    "side" to intent.side.name,
                    "budget" to intent.budgetUsd,
                    "market" to intent.market,
                    "decision_probability" to report.probability,
                    "reasons" to report.reasons
                ))
            }
            is Intent.ScanMarket -> Action(id = genId(), type = "scan_market", meta = mapOf("scope" to intent.scope))
            is Intent.AddMemory -> {
                // persist memory locally
                localStore?.saveMemory(intent.title, intent.text)
                Action(id = genId(), type = "add_memory", meta = mapOf("title" to intent.title))
            }
            is Intent.QueryStatus -> Action(id = genId(), type = "query_status", meta = emptyMap<String, Any?>())
            is Intent.Shutdown -> Action(id = genId(), type = "shutdown", meta = emptyMap<String, Any?>())
            else -> Action(id = genId(), type = "unknown", meta = emptyMap<String, Any?>())
        }
    }

    fun registerPendingAction(action: Action) {
        pendingActions[action.id] = action
        // persist pending action if required
    }

    fun confirmAndExecute(actionId: String, executor: (Action) -> Unit): Boolean {
        val a = pendingActions[actionId] ?: return false
        // Security checks / budget guards should be applied by executor
        executor(a)
        pendingActions.remove(actionId)
        return true
    }

    fun cancelPending(actionId: String) {
        pendingActions.remove(actionId)
    }

    fun listPending(): List<Action> = pendingActions.values.toList()

    private fun estimateTradeProbability(tradeIntent: Intent.TradeRequest): DecisionReport {
        // Placeholder heuristic combining randomness + simple signals.
        // IMPORTANT: replace with real ML scoring or remote model for production.
        val base = 0.4
        val budgetBoost = when {
            tradeIntent.budgetUsd == null -> 0.0
            tradeIntent.budgetUsd >= 50 -> 0.2
            tradeIntent.budgetUsd >= 20 -> 0.1
            else -> 0.05
        }
        val marketSignal = when (tradeIntent.market) {
            "crypto" -> 0.15
            "forex" -> 0.1
            "quotex" -> 0.12
            else -> 0.05
        }
        val randomness = Random.nextDouble(0.0, 0.2)
        val probability = (base + budgetBoost + marketSignal + randomness).coerceIn(0.0, 1.0)
        val reasons = listOf("budgetFactor=$budgetBoost", "marketSignal=$marketSignal", "randomness=${"%.2f".format(randomness)}")
        return DecisionReport(probability = probability, reasons = reasons)
    }

    private fun genId(): String = "act_" + System.currentTimeMillis().toString(36)
}
