package com.shahrafuking.kingassistant.voice

/**
 * Sealed command types parsed from voice input.
 */
sealed class Command {
    data class Trade(val amount: Double, val currency: String = "USD") : Command()
    data class SetBudget(val amount: Double, val currency: String = "USD") : Command()
    object PanicStop : Command()
    data class CancelTrade(val id: String) : Command()
    data class QueryStatus(val subject: String? = null) : Command()
    data class Unknown(val raw: String) : Command()
}
