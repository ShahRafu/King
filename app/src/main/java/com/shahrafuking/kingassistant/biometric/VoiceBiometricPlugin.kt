package com.shahrafuking.kingassistant.biometric

interface VoiceBiometricPlugin {
    fun enroll(audio: ByteArray): Boolean
    fun verify(audio: ByteArray): Float
}

class VoiceBiometricPluginImpl : VoiceBiometricPlugin {
    override fun enroll(audio: ByteArray): Boolean {
        // placeholder - implement real enrollment
        return true
    }

    override fun verify(audio: ByteArray): Float {
        // placeholder verification score
        return 0.0f
    }
}
