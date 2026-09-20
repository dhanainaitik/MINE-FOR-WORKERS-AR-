package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Fire Hazard Model:
 * - Loads the refined animated fire GLB model: animated_fire (1).glb (with animated_fire.glb fallback)
 * - Features 64-channel continuous, bell-curved flame morphing with zero stuck or frozen meshes
 * - Plays with optimized thermal flow rate (1.35x playback speed)
 * - Lightweight multi-harmonic secondary procedural motion creates living thermal convection,
 *   subtle lateral draft sway, and organic upward flame surging
 * - Grounded at floor level (Y = 0.0f) beside the railway track
 * - Positioned adjacent to existing industrial electrical equipment (Object_9) at ~10.5m down the drift
 * - Visual only: continuously burns indefinitely without stopping or extinguishing
 */
class AnimatedFireNode(
    engine: Engine,
    modelLoader: ModelLoader,
    var baseScale: Float3 = Float3(1.75f, 1.75f, 1.75f)
) : Node(engine) {

    private val tag = "AnimatedFireNode"
    var fireModelNode: ModelNode? = null
        private set

    private var startNanos: Long = -1L

    init {
        try {
            // Attempt to load using the exact supplied file name first, falling back to normalized name
            val fireInstance = try {
                modelLoader.createModelInstance("models/animated_fire (1).glb")
            } catch (e: Exception) {
                Log.w(tag, "Failed to load models/animated_fire (1).glb directly (${e.message}), loading models/animated_fire.glb")
                modelLoader.createModelInstance("models/animated_fire.glb")
            } ?: modelLoader.createModelInstance("models/animated_fire.glb")

            val modelNode = ModelNode(
                modelInstance = fireInstance,
                autoAnimate = true
            ).apply {
                // Dimensions in GLB are width 0.86m, height 1.06m, depth 0.62m.
                // Positioned directly below and around the lower portion of the wall-mounted electrical box.
                position = Float3(0.0f, 0.0f, 0.0f)
                rotation = Float3(0.0f, 0.0f, 0.0f)
                scale = baseScale

                // Filter out any static campfire logs/grates so only pure animated electrical flames remain
                try {
                    renderableNodes.forEach { rNode ->
                        val name = rNode.name
                        if (name != null && (name.contains("Log", ignoreCase = true) ||
                                             name.contains("Grate", ignoreCase = true) ||
                                             name.contains("Ashes", ignoreCase = true))) {
                            rNode.isVisible = false
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "RenderableNode filter: ${e.message}")
                }

                // Inspect and automatically start continuous indefinite animation playback
                val count = animationCount
                Log.i(tag, "Detected $count animation clip(s) in animated_fire GLB")
                if (count > 0) {
                    playAnimation(animationIndex = 0, loop = true)
                    // Moderate playback speed increase for authentic fluid flame convection
                    setAnimationSpeed(0, 1.35f)
                    Log.i(tag, "Animation clip 0 started with continuous looping at 1.35x speed")
                } else {
                    Log.w(tag, "Warning: No animation clip detected in animated_fire GLB")
                }
            }

            addChildNode(modelNode)
            fireModelNode = modelNode
            Log.i(tag, "AnimatedFireNode successfully initialized and added to scene with baseScale $baseScale")
        } catch (e: Exception) {
            Log.e(tag, "Fatal error loading animated fire model: ${e.message}", e)
        }
    }

    fun stopAnimation() {
        try {
            if ((fireModelNode?.animationCount ?: 0) > 0) {
                fireModelNode?.stopAnimation(0)
                Log.i(tag, "Animated fire animation playback stopped")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to stop fire animation: ${e.message}")
        }
    }

    override fun destroy() {
        stopAnimation()
        super.destroy()
    }

    /**
     * Continuous lightweight procedural secondary motion:
     * Simulates natural aerodynamic thermal draft, flame surging, and buoyant convection
     * to guarantee that the entire flame feels continuously alive, dynamic, and fluid.
     */
    override fun onFrame(frameTimeNanos: Long) {
        if (!isVisible) return
        super.onFrame(frameTimeNanos)
        if (startNanos < 0) startNanos = frameTimeNanos
        val t = (frameTimeNanos - startNanos).toDouble() / 1_000_000_000.0

        val model = fireModelNode ?: return

        // 1. Multi-harmonic flame stretch & contraction (upward heat thermal surging scaled with baseScale)
        val stretchY = baseScale.y * (1.0f + (0.040f * sin(t * 5.2).toFloat()) +
                                              (0.022f * sin(t * 8.7 + 1.1).toFloat()))
        val squashXZ = baseScale.x * (1.0f - (0.020f * sin(t * 5.2).toFloat()) +
                                              (0.015f * cos(t * 7.3).toFloat()))
        model.scale = Float3(squashXZ, stretchY, squashXZ)

        // 2. Subtle buoyant vertical pulsation & draft drift
        val buoyantY = (0.015f * sin(t * 4.6).toFloat()).coerceAtLeast(0.0f)
        val swayX = 0.012f * sin(t * 3.8).toFloat() + 0.005f * sin(t * 7.1).toFloat()
        val swayZ = 0.010f * cos(t * 3.3).toFloat()
        model.position = Float3(swayX, buoyantY, swayZ)

        // 3. Subtle natural flame tilt & micro-flicker (draft currents in mine drift)
        val tiltX = 1.4f * sin(t * 4.2).toFloat()
        val tiltY = 2.0f * sin(t * 3.1).toFloat()
        val tiltZ = 1.6f * cos(t * 4.9).toFloat()
        model.rotation = Float3(tiltX, tiltY, tiltZ)
    }
}
