package com.shahrafuking.kingassistant.voice

import androidx.test.core.app.ApplicationProvider
import com.shahrafuking.kingassistant.voice.tflite.SpeakerVerifier
import org.junit.Assert.*
import org.junit.Test

class SpeakerVerifierTest {
    @Test
    fun testFallbackEmbeddingAndSimilarity() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val verifier = SpeakerVerifier(ctx, modelAssetPath = null)

        val f1 = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val f2 = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val fe1 = verifier.computeEmbedding(f1)
        val fe2 = verifier.computeEmbedding(f2)
        assertNotNull(fe1)
        assertNotNull(fe2)
        val s = verifier.cosineSimilarity(fe1!!, fe2!!)
        assertTrue(s > 0.999f) // identical inputs => near 1.0 similarity

        // different vector
        val f3 = floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f)
        val fe3 = verifier.computeEmbedding(f3)
        assertNotNull(fe3)
        val s2 = verifier.cosineSimilarity(fe1, fe3!!)
        assertTrue(s2 < 0.8f)
    }
}
