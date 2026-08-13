package com.shahrafuking.kingassistant.trading

import com.shahrafuking.kingassistant.trading.PanicStopManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * TradeSimulator: a safe, local-only simulator implementing TradeAdapter.
 * - Simulates fills after a short delay.
 * - Supports cancellation and responds to PanicStopManager by cancelling in-flight orders.
 * - NEVER performs any network or broker calls.
 */
class TradeSimulator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TradeAdapter {
    private val _events = MutableSharedFlow<OrderEvent>(replay = 0, extraBufferCapacity = 50)
    override val orderEvents = _events.asSharedFlow()

    // Keep track of running jobs by orderId so we can cancel them
    private val jobs = ConcurrentHashMap<String, Job>()

    init {
        // Listen for global panic-stop events and cancel running orders
        scope.launch {
            PanicStopManager.events.collect {
                // cancel all running order jobs
                jobs.values.forEach { it.cancel() }
                val ids = jobs.keys.toList()
                jobs.clear()
                // Emit cancelled events for each
                ids.forEach { id ->
                    _events.tryEmit(OrderEvent.Cancelled(id))
                }
            }
        }
    }

    override suspend fun submitOrder(order: Order): String {
        _events.emit(OrderEvent.Submitted(order))
        // schedule a simulated fill
        val job = scope.launch {
            try {
                // variable delay to simulate market latency
                val delayMs = 500L + Random.nextLong(300L, 1200L)
                delay(delayMs)
                // if not cancelled, emit filled event
                _events.emit(OrderEvent.Filled(order.id, order.amount))
                // remove from jobs
                jobs.remove(order.id)
            } catch (e: CancellationException) {
                // on cancellation, emit Cancelled if not already emitted by PanicStop
                _events.emit(OrderEvent.Cancelled(order.id))
                jobs.remove(order.id)
            } catch (t: Throwable) {
                _events.emit(OrderEvent.Failed(order.id, t.localizedMessage ?: "error"))
                jobs.remove(order.id)
            }
        }
        jobs[order.id] = job
        return order.id
    }

    override suspend fun cancelOrder(orderId: String): Boolean {
        val j = jobs.remove(orderId)
        if (j != null) {
            j.cancel()
            _events.emit(OrderEvent.Cancelled(orderId))
            return true
        }
        // already finished or unknown
        return false
    }

    fun shutdown() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }
}
