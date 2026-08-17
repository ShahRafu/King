package com.shahrafuking.kingassistant.stubs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RobotEngine {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            doWork()
        }
    }

    private suspend fun doWork() {
        // Example use of delay; adapt to your logic
        delay(500L)
        // actual work here
    }

    fun stop() {
        // stop logic if needed
    }
}
