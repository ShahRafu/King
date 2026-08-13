package com.shahrafuking.kingassistant.trading

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PanicStopManager: singleton that broadcasts panic-stop events to listeners and
 * maintains a boolean flag. Clients should cooperatively cancel/stop work when
 * a panic is triggered.
 */
object PanicStopManager {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()
    private val triggered = AtomicBoolean(false)

    fun triggerPanicStop() {
        if (triggered.compareAndSet(false, true)) {
            _events.tryEmit(Unit)
        }
    }

    fun isTriggered(): Boolean = triggered.get()

    fun clear() {
        triggered.set(false)
    }
}
