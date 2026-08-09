package com.shahrafuking.kingassistant.core

/**
 * Platform-agnostic interfaces to keep core logic portable to desktop/laptop later.
 * Implement Android adapters that use these interfaces; core modules should depend
 * on these abstractions rather than Android classes directly.
 */
interface PlatformAudioManager {
    fun hasRecordPermission(): Boolean
    fun requestRecordPermission()
}

interface ForegroundController {
    fun startForeground()
    fun stopForeground()
}
