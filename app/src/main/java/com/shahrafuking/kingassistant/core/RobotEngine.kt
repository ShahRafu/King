package com.shahrafuking.kingassistant.core

import com.shahrafuking.kingassistant.storage.LocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RobotEngine
 * - Top-level orchestrator: receives user inputs (text/voice), coordinates IntentParser & DialogueManager,
 *   and exposes safe hooks for actual external actions (e.g., trade execution).
 */

class RobotEngine(private val context: android.content.Context) {

    private val dialogueManager = DialogueManager(context)
    private val localStore = LocalStore(context)
    private val voiceAuth = com.shahrafuking.kingassistant.security.VoiceAuthStub(context as androidx.activity.ComponentActivity)
    private val modelClient = com.shahrafuking.kingassistant.model.ModelClient(baseUrl = "https://example.ai/infer", apiKey = null) // placeholder

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Process textual user input synchronously (returns immediate scaffold response).
     */
    fun processTextInput(text: String): String {
        val intent = IntentParser.parse(text)
        val action = dialogueManager.parseIntentToAction(intent)

        when (action.type) {
            "auth_request" -> {
                return "ভয়েস লগইনের জন্য প্রস্তুত — ভয়েস‑লগইন বোতাম টিপুন।"
            }
            "panic_stop" -> {
                return "প্যানিক স্টপ ধার্য করা হলো — সকল pending actions বাতিল করুন এবং এক্সিকিউট halt[...]"
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
                dialogueManager.registerPendingAction(action)
                return "ট্রেড পরিকল্পনা প্রস্তুত হয়েছে (actionId=${action.id}). অনুমিত জয়ের সম্ভাবনা[...]"
            }
            "scan_market" -> {
                scheduleMarketScan(action)
                return "স্ক্যানিং শুরু করা হলো (স্কোপ: ${action.meta["scope"]}). ফলাফল পরে দেখানো হবে।"
            }
            "add_memory" -> {
                return "���পনার মেমোরি সেভ করা হয়েছে।"
            }
            "query_status" -> {
                val pending = dialogueManager.listPending()
                val budget = localStore.getUserPreference("budget_usd") ?: "নির্ধারিত নেই"
                return "বর্তমান বাজেট: $budget; Pending actions: ${pending.size}"
            }
            "shutdown" -> {
                return "King Assistant নিষ্ক্রিয় করার জন্য প্রস্তুত — কনফার্মেশন কণ্ঠ দিন।"
            }
            else -> {
                val reply = requestLLM(text)
                return reply ?: "আমি বুঝতে পারিনি — একটু সরলভাবে বললে ভালো হয়।"
            }
        }
    }

    fun confirmPendingAction(actionId: String, onExecuted: (Boolean, String) -> Unit) {
        val executor: (Action) -> Unit = { act ->
            // simulate
        }

        val success = dialogueManager.confirmAndExecute(actionId, executor)
        if (success) onExecuted(true, "Action executed (simulated). Implement real executor to perform live actions.")
        else onExecuted(false, "Action not found or already executed/cancelled.")
    }

    fun enrollVoiceAuth(onComplete: (Boolean) -> Unit) {
        onComplete(true)
    }

    private fun scheduleMarketScan(action: Action) {
        coroutineScope.launch {
            // Placeholder: heavy work should be done on remote worker
            delay(2000)
            // store scan result in localStore or send notification
        }
    }

    private fun requestLLM(text: String): String? {
        return "মডেল‑উত্তর (ডেমো): '${text.take(80)}'"
    }
}
