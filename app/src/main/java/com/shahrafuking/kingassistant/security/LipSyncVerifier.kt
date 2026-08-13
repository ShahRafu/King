package com.shahrafuking.kingassistant.security

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shahrafuking.kingassistant.capture.AVSyncProcessor
import com.shahrafuking.kingassistant.capture.AudioCaptureHelper

/**
 * LipSyncVerifier
 *
 * Uses ML Kit Face Detection to extract mouth contours and AVSyncProcessor to compute a sync score
 * between audio and mouth motion. This implementation captures camera frames via CameraXImageCollector
 * (a small helper that uses CameraX ImageAnalysis) and records audio for the same duration, then
 * runs the AV-sync model to compute a score.
 */
class LipSyncVerifier(private val context: Context) {
    private val TAG = "LipSyncVerifier"
    private val audioHelper = AudioCaptureHelper()
    private val avsync = AVSyncProcessor(context)

    data class LipResult(val success: Boolean, val score: Float, val reason: String?)

    suspend fun verifyLipSync(activity: Activity, durationMs: Long = 3000, threshold: Float = 0.75f): LipResult = withContext(Dispatchers.Main) {
        try {
            // Setup ML Kit face detector
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
            val detector = FaceDetection.getClient(options)

            // Collect camera frames for durationMs
            val imageCollector = CameraXImageCollector(activity, durationMs)
            val frames = imageCollector.collectFrames()

            // Record audio concurrently (blocking until done)
            val audioBytes = audioHelper.recordForDurationMs(durationMs)

            // Extract mouth landmarks from frames
            val mouthLandmarks = ArrayList<Float>()
            for (img in frames) {
                try {
                    val facesTask = detector.process(img)
                    val faces = kotlinx.coroutines.suspendCancellableCoroutine<List<com.google.mlkit.vision.face.Face>> { cont ->
                        facesTask.addOnSuccessListener { faces -> cont.resume(faces) {} }
                        facesTask.addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
                    }
                    if (faces.isNotEmpty()) {
                        val f = faces[0]
                        val lower = f.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points
                        val upper = f.getContour(FaceContour.UPPER_LIP_TOP)?.points
                        if (upper != null && lower != null) {
                            val upCount = upper.size
                            val lowCount = lower.size
                            for (i in 0 until 3) {
                                val ui = (i * (upCount - 1) / 2)
                                val li = (i * (lowCount - 1) / 2)
                                val up = upper[ui]
                                val lp = lower[li]
                                mouthLandmarks.add(up.x)
                                mouthLandmarks.add(up.y)
                                mouthLandmarks.add(lp.x)
                                mouthLandmarks.add(lp.y)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "frame processing failed", e)
                }
            }

            if (mouthLandmarks.isEmpty()) {
                return@withContext LipResult(false, 0f, "no_mouth_landmarks")
            }

            val landmarksArray = FloatArray(mouthLandmarks.size)
            for (i in mouthLandmarks.indices) landmarksArray[i] = mouthLandmarks[i]

            // Compute AV-sync score
            val score = avsync.computeSyncScore(audioBytes, landmarksArray)
            val success = score >= threshold
            return@withContext LipResult(success, score, if (success) null else "low_sync_score")
        } catch (e: Exception) {
            Log.e(TAG, "verifyLipSync failed", e)
            return@withContext LipResult(false, 0f, "exception:${e.message}")
        }
    }
}
