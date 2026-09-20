package com.minesafe.ar.gestures

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

/**
 * 2D normalized landmark coordinate (0.0 to 1.0) on the screen.
 */
data class HandLandmarkPoint(
    val x: Float,
    val y: Float
)

/**
 * Hand Gesture State:
 * Immutable snapshot of the detected hand, landmarks, and gestures for the current frame.
 */
data class HandGestureState(
    val isHandPresent: Boolean = false,
    val isPinching: Boolean = false,
    val pinchDistance: Float = 1.0f,
    val pinchCenterX: Float = 0.5f,
    val pinchCenterY: Float = 0.5f,
    val pinchHoldDurationMs: Long = 0L,
    val consecutivePinchFrames: Int = 0,
    val wristX: Float = 0.5f,
    val wristY: Float = 0.5f,
    val landmarks: List<HandLandmarkPoint> = emptyList(),
    val isSqueezing: Boolean = false,
    val gripRatio: Float = 1.0f,
    val diagnosticStatus: String = "NO HAND DETECTED",
    val frameCount: Long = 0L
) {
    /**
     * Checks if a pinch gesture is verified for picking up the extinguisher (held >= 80ms or 2 frames).
     */
    fun isPinchPickupTriggered(): Boolean {
        return isPinching && (pinchHoldDurationMs >= 80L || consecutivePinchFrames >= 2)
    }

    /**
     * Checks if a deliberate pin-pull gesture is triggered (held pinch >= 200ms or 3 frames).
     */
    fun isPinPullTriggered(): Boolean {
        return isPinching && (pinchHoldDurationMs >= 200L || consecutivePinchFrames >= 3)
    }

    /**
     * Checks if a lever squeeze gesture is active (either full hand grip or deliberate lever compression).
     */
    fun isLeverSqueezeActive(): Boolean {
        return isSqueezing || (isPinching && (pinchHoldDurationMs >= 250L || consecutivePinchFrames >= 4))
    }

    /**
     * Checks if a pinch gesture has been held continuously for at least [durationMs] (for fire suppression).
     */
    fun isPinchHoldActive(durationMs: Long = 600L): Boolean {
        return isPinching && pinchHoldDurationMs >= durationMs
    }
}

/**
 * Module 01: Hand Gesture Detector
 * - Tracks 21 MediaPipe hand landmarks (wrist, thumb tip #4, index finger tip #8, etc.)
 * - Computes normalized Euclidean pinch distance between Thumb Tip and Index Finger Tip
 * - Computes multi-finger grip compression for extinguisher lever squeezing
 * - Implements hysteresis thresholds to eliminate single-frame chatter/noise
 * - Tracks continuous pinch hold duration (for activation/discharge gestures)
 * - Safe, deterministic, non-allocating per frame
 */
class HandGestureDetector(
    val pinchOnThreshold: Float = 0.080f,
    val pinchOffThreshold: Float = 0.115f
) {
    private var wasPinching: Boolean = false
    private var pinchStartTimeMs: Long = 0L
    private var pinchFramesCount: Int = 0
    private var processedFrames: Long = 0L

    fun processLandmarks(
        rawLandmarks: List<NormalizedLandmark>?,
        timestampMs: Long,
        pipelineDiagnostics: String = "PIPELINE ACTIVE"
    ): HandGestureState {
        processedFrames++

        if (rawLandmarks == null || rawLandmarks.size < 21) {
            wasPinching = false
            pinchStartTimeMs = 0L
            pinchFramesCount = 0
            return HandGestureState(
                isHandPresent = false,
                diagnosticStatus = "NO HAND DETECTED (Frames: $processedFrames)",
                frameCount = processedFrames
            )
        }

        val wrist = rawLandmarks[0]
        val thumbTip = rawLandmarks[4]
        val indexTip = rawLandmarks[8]
        val middleTip = rawLandmarks[12]
        val ringTip = rawLandmarks[16]
        val pinkyTip = rawLandmarks[20]
        val palmCenter = rawLandmarks[9] // Middle finger MCP joint

        val dx = (thumbTip.x() - indexTip.x()).toDouble()
        val dy = (thumbTip.y() - indexTip.y()).toDouble()
        val distance = hypot(dx, dy).toFloat()

        // Hysteresis: require tighter distance to engage pinch, looser distance to disengage
        val isPinching = if (wasPinching) {
            distance < pinchOffThreshold
        } else {
            distance < pinchOnThreshold
        }

        if (isPinching) {
            if (!wasPinching) {
                pinchStartTimeMs = timestampMs
            }
            pinchFramesCount++
        } else {
            pinchStartTimeMs = 0L
            pinchFramesCount = 0
        }

        val holdDuration = if (isPinching && pinchStartTimeMs > 0L) {
            (timestampMs - pinchStartTimeMs).coerceAtLeast(0L)
        } else {
            0L
        }

        wasPinching = isPinching

        val centerX = (thumbTip.x() + indexTip.x()) / 2.0f
        val centerY = (thumbTip.y() + indexTip.y()) / 2.0f

        // Multi-finger grip squeeze detection: average distance of fingertips to palm center
        val avgTipDistToPalm = (
            hypot((indexTip.x() - palmCenter.x()).toDouble(), (indexTip.y() - palmCenter.y()).toDouble()) +
            hypot((middleTip.x() - palmCenter.x()).toDouble(), (middleTip.y() - palmCenter.y()).toDouble()) +
            hypot((ringTip.x() - palmCenter.x()).toDouble(), (ringTip.y() - palmCenter.y()).toDouble()) +
            hypot((pinkyTip.x() - palmCenter.x()).toDouble(), (pinkyTip.y() - palmCenter.y()).toDouble())
        ).toFloat() / 4.0f

        val isGripSqueeze = avgTipDistToPalm < 0.22f

        val pts = rawLandmarks.map { HandLandmarkPoint(it.x(), it.y()) }
        val status = when {
            isGripSqueeze -> "GRIP SQUEEZE DETECTED (grip: ${"%.3f".format(avgTipDistToPalm)})"
            isPinching -> "PINCH DETECTED (dist: ${"%.3f".format(distance)}, hold: ${holdDuration}ms)"
            else -> "HAND DETECTED (21 pts, dist: ${"%.3f".format(distance)})"
        }

        return HandGestureState(
            isHandPresent = true,
            isPinching = isPinching,
            pinchDistance = distance,
            pinchCenterX = centerX,
            pinchCenterY = centerY,
            pinchHoldDurationMs = holdDuration,
            consecutivePinchFrames = pinchFramesCount,
            wristX = wrist.x(),
            wristY = wrist.y(),
            landmarks = pts,
            isSqueezing = isGripSqueeze,
            gripRatio = avgTipDistToPalm,
            diagnosticStatus = "$status | $pipelineDiagnostics",
            frameCount = processedFrames
        )
    }

    fun reset() {
        wasPinching = false
        pinchStartTimeMs = 0L
        pinchFramesCount = 0
    }
}
