package com.shahrafuking.kingassistant.selfheal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SelfHealingManager
 *
 * High-level coordinator that glues together:
 * - error detection (external to this scaffold; provide errors to reportError)
 * - file isolation/backups via FileIsolator
 * - voice-authorized staging via VoiceAuthGatekeeper
 * - optional local execution of guarded JS plugins via LocalCodeExecutor
 *
 * Usage:
 * val mgr = SelfHealingManager(activity)
 * mgr.reportError("stacktrace or compiler output...")
 */
class SelfHealingManager(private val activity: Context) {
    private val TAG = "SelfHealingManager"
    private val isolator = FileIsolator(activity)
    private val gatekeeper = VoiceAuthGatekeeper(activity as android.app.Activity)
    private val executor = LocalCodeExecutor()

    suspend fun reportError(errorText: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "reportError called")
        val candidate = isolator.identifyFileForError(errorText)
        if (candidate == null) {
            Log.w(TAG, "Could not identify a single file from error")
            return@withContext false
        }

        // Backup if the file exists on disk
        val backup = isolator.backupFileOnDisk(candidate)
        Log.i(TAG, "backup created: $backup")

        // Generate a conservative patch suggestion (placeholder)
        val existing = try {
            java.io.File(candidate).readText()
        } catch (t: Throwable) {
            "" // file might not be present in device FS; treat as new
        }

        val proposed = generateConservativePatch(existing, errorText)

        // Present proposed patch to owner via TTS/UI and request voice approval
        val prompt = "A proposed fix for $candidate is ready. Say the challenge phrase to approve applying the fix."
        val approved = gatekeeper.requestOwnerApproval(prompt)
        if (!approved) {
            Log.i(TAG, "Owner did not approve the patch")
            return@withContext false
        }

        // Stage the change to app-internal storage
        val stagedPath = isolator.stageFileContents(java.io.File(candidate).name, proposed)
        Log.i(TAG, "staged change at $stagedPath")

        // Optionally ran small JS validators/plugins on the proposed text (example usage)
        val validationScript = """
        // The plugin receives 'content' string and must return true for pass
        (function(content){
            // simple heuristic: ensure no network strings
            return (!/fetch|require|(net\.)|(http)/.test(content));
        })(content)
        """
        val ok = executor.runValidator(validationScript, mapOf("content" to proposed))
        if (!ok) {
            Log.w(TAG, "Staged patch failed local validation")
            return@withContext false
        }

        // At this point the staged change exists in internal storage. Owner should run a local build/test.
        // Provide instructions via UI to run `./gradlew test` or `./gradlew assembleDebug`. The scaffold will not
        // automatically overwrite checked-in source files or commit to git without explicit owner steps.

        return@withContext true
    }

    private fun generateConservativePatch(existing: String, errorText: String): String {
        // Placeholder strategy: append a TODO + comment with the error near the top of the file.
        val timestamp = System.currentTimeMillis()
        val cleanError = errorText.replace("/", "")
        val header = "/* SELF-HEAL PROPOSAL: $timestamp */\n/* Error: $cleanError */\n\n"
        
        return header + existing
    }
}
