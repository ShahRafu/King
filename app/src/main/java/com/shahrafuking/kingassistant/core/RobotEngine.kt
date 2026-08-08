package com.shahrafuking.kingassistant.core

import com.shahrafuking.kingassistant.storage.LocalStore

/**
 * RobotEngine
 * - Top-level orchestrator: receives user inputs (text/voice), coordinates IntentParser & DialogueManager,
 *   and exposes safe hooks for actual external actions (e.g., trade execution).
 *
 * Important safety behavior:
 * - This engine will NOT auto-execute trade actions without explicit voice confirmation (panic stop & confirmation required).
 * - Hooks for controversial features (auto-click, IP rotation) are intentionally empty and documented as implementer-only.
 */

class RobotEngine(private val context: android.content.Context) {

    private val dialogueManager = DialogueManager(context)
    private val localStore = LocalStore(context)
    private val voiceAuth = com.shahrafuking.kingassistant.security.VoiceAuthStub(context as androidx.activity.ComponentActivity)
    private val modelClient = com.shahrafuking.kingassistant.model.ModelClient(baseUrl = "https://example.ai/infer", apiKey = null) // placeholder

    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    /**
     * Process textual user input synchronously (returns immediate scaffold response).
     * For trade intents, it will create pending action and return the action id and decision info.
     */
    fun processTextInput(text: String): String {
        val intent = IntentParser.parse(text)
        val action = dialogueManager.parseIntentToAction(intent)

        when (action.type) {
            "auth_request" -> {
                // trigger voice auth flow externally
                return "ভয়েস লগইনের জন্য প্রস্তুত — ভয়েস‑লগইন বোতাম টিপুন।"
            }
            "panic_stop" -> {
                // create immediate stop instruction (no external harmful code)
                // Implementer should wire this to trade-execution engine to halt positions.
                // Here we just return scaffold.
                return "প্যানিক স্টপ ধার্য করা হলো — সকল pending actions বাতিল করুন এবং এক্সিকিউট halt hook কল করুন।"
            }
            "set_budget" -> {
                val budget = action.meta["budget"]
                if (budget is Double) {
                    localStore.saveUserPreference("budget_usd", budget.toString())
                    return "বাজেট ${budget} USD হিসেবে সেট করা সাফল্য।"
                }
                return "বাজেট সেট করা যায়নি — অনুগ্রহ করে আবার চেষ্টা করুন।"
            }
            "trade_intent" -> {
                // register pending action and return details (requires explicit voice confirmation)
                dialogueManager.registerPendingAction(action)
                val prob = action.meta["decision_probability"] as? Double ?: 0.0
                val reasons = action.meta["reasons"] as? List<*>
                return "ট্রেড পরিকল্পনা প্রস্তুত হয়েছে (actionId=${action.id}). অনুমিত জয়ের সম্ভাবনা: ${(prob * 100).toInt()}%। অনুমোদনের জন্য ভয়েস কনফার্মেশন দিন। কারণ: ${reasons?.joinToString()}"
            }
            "scan_market" -> {
                // schedule background scanning (placeholder)
                scheduleMarketScan(action)
                return "স্ক্যানিং শুরু করা হলো (স্কোপ: ${action.meta["scope"]}). ফলাফল পরে দেখানো হবে।"
            }
            "add_memory" -> {
                return "আপনার মেমোরি সেভ করা হয়েছে।"
            }
            "query_status" -> {
                // compose status from localStore + pending actions
                val pending = dialogueManager.listPending()
                val budget = localStore.getUserPreference("budget_usd") ?: "নির্ধারিত নেই"
                return "বর্তমান বাজেট: $budget; Pending actions: ${pending.size}"
            }
            "shutdown" -> {
                // safe shutdown scaffold
                return "King Assistant নিষ্ক্রিয় করার জন্য প্রস্তুত — কনফার্মেশন কণ্ঠ দিন।"
            }
            else -> {
                // Fallback: query model for general chat (placeholder)
                val reply = requestLLM(text)
                return reply ?: "আমি বুঝতে পারিনি — একটু সরলভাবে বললে ভালো হয়।"
            }
        }
    }

    /**
     * External method to confirm a pending action via voice authentication.
     * This should be called after voiceAuth verifies the user's voice.
     */
    fun confirmPendingAction(actionId: String, onExecuted: (Boolean, String) -> Unit) {
        // double-check voiceAuth / settings (this function assumes pre-validated voice)
        // Provide an executor hook for implementer to perform the actual action.
        val executor: (Action) -> Unit = { act ->
            // WARNING: implementer must implement real trade execution and security checks.
            // Here we only log and simulate success.
            // e.g., call TradingAdapter.executeTrade(...)
        }

        val success = dialogueManager.confirmAndExecute(actionId, executor)
        if (success) onExecuted(true, "Action executed (simulated). Implement real executor to perform live actions.")
        else onExecuted(false, "Action not found or already executed/cancelled.")
    }

    fun enrollVoiceAuth(onComplete: (Boolean) -> Unit) {
        // Placeholder - enrollment flow should store voice-print embeddings securely
        onComplete(true)
    }

    private fun scheduleMarketScan(action: Action) {
        coroutineScope.launch {
            // Placeholder: heavy work should be done on remote worker (Colab / Kaggle / self-hosted)
            kotlinx.coroutines.delay(2000)
            // store scan result in localStore or send notification
        }
    }

    private fun requestLLM(text: String): String? {
        // Placeholder synchronous wrapper. In production use async callbacks.
        // ModelClient.requestCompletion should be used asynchronously.
        return "মডেল‑উত্তর (ডেমো): '${text.take(80)}'"
    }
}
