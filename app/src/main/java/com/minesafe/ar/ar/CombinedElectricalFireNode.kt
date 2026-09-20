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
 * Combined Electrical Fire Hazard Node:
 * - Loads the unified single hazard model: Optimized_Fire(tera-mine)
 * - Contains BOTH the industrial electrical box and the actively burning fire/smoke in one coherent asset
 * - Eliminates separate electrical box and separate fire nodes (zero duplicate instances)
 * - Wall-mounted at chest level against the mine wall / timber support arch
 * - Supports programmatic animation start, continuous looping, and stopping for training flow
 * - Strictly module-scoped to Module 01 (Electrical Fire Safety); never displayed in Module 02
 */
class CombinedElectricalFireNode(
    engine: Engine,
    modelLoader: ModelLoader,
    var baseScale: Float3 = Float3(0.050f, 0.050f, 0.050f)
) : Node(engine) {

    private val tag = "CombinedFireNode"
    var hazardModelNode: ModelNode? = null
        private set

    private var isAnimationRunning: Boolean = true
    private var startNanos: Long = -1L

    init {
        try {
            // Load the unified combined asset from assets/models
            val hazardInstance = try {
                modelLoader.createModelInstance("models/Optimized_Fire(tera-mine)")
            } catch (e: Exception) {
                Log.w(tag, "Failed to load models/Optimized_Fire(tera-mine) directly (${e.message}), trying .glb extension")
                try {
                    modelLoader.createModelInstance("models/Optimized_Fire(tera-mine).glb")
                } catch (e2: Exception) {
                    modelLoader.createModelInstance("models/optimized_fire.glb")
                }
            } ?: try {
                modelLoader.createModelInstance("models/Optimized_Fire(tera-mine).glb")
            } catch (e3: Exception) {
                modelLoader.createModelInstance("models/optimized_fire.glb")
            }

            val modelNode = ModelNode(
                modelInstance = hazardInstance,
                autoAnimate = true
            ).apply {
                position = Float3(0.0f, 0.0f, 0.0f)
                rotation = Float3(0.0f, 0.0f, 0.0f)
                scale = baseScale

                // Automatically start and loop all animation clips contained in the GLB
                val count = animationCount
                Log.i(tag, "Detected $count animation clip(s) in Optimized_Fire(tera-mine)")
                for (i in 0 until count) {
                    playAnimation(animationIndex = i, loop = true)
                    setAnimationSpeed(i, 1.25f)
                    Log.i(tag, "Animation clip $i started with continuous looping at 1.25x speed")
                }
            }

            addChildNode(modelNode)
            hazardModelNode = modelNode
            isAnimationRunning = true
            Log.i(tag, "CombinedElectricalFireNode successfully initialized and added to scene")
        } catch (e: Exception) {
            Log.e(tag, "Fatal error loading combined electrical fire model: ${e.message}", e)
        }
    }

    fun isFireAnimationRunning(): Boolean = isAnimationRunning

    /**
     * Start/Resume continuous looping fire animation.
     */
    fun startFireAnimation() {
        try {
            isAnimationRunning = true
            val model = hazardModelNode ?: return
            val count = model.animationCount
            for (i in 0 until count) {
                model.playAnimation(animationIndex = i, loop = true)
                model.setAnimationSpeed(i, 1.25f)
            }
            Log.i(tag, "startFireAnimation: resumed $count animation clips with continuous looping")
        } catch (e: Exception) {
            Log.w(tag, "Failed to start fire animation: ${e.message}")
        }
    }

    /**
     * Stop/Pause fire animation.
     * Keeps the hazard model in the scene, stops flame movement, and resets transforms cleanly.
     */
    fun stopFireAnimation() {
        try {
            isAnimationRunning = false
            val model = hazardModelNode ?: return
            val count = model.animationCount
            for (i in 0 until count) {
                model.stopAnimation(i)
            }
            // Reset to base transform so no skewed frozen frame remains
            model.scale = baseScale
            model.position = Float3(0.0f, 0.0f, 0.0f)
            model.rotation = Float3(0.0f, 0.0f, 0.0f)
            Log.i(tag, "stopFireAnimation: stopped $count animation clips")
        } catch (e: Exception) {
            Log.w(tag, "Failed to stop fire animation: ${e.message}")
        }
    }

    fun stopAnimation() {
        stopFireAnimation()
    }

    override fun destroy() {
        stopFireAnimation()
        super.destroy()
    }

    /**
     * Continuous lightweight procedural secondary thermal motion:
     * Simulates natural aerodynamic thermal draft, flame surging, and buoyant convection
     * while the fire is active. Completely paused when fire is stopped.
     */
    override fun onFrame(frameTimeNanos: Long) {
        if (!isVisible || !isAnimationRunning) return
        super.onFrame(frameTimeNanos)
        if (startNanos < 0) startNanos = frameTimeNanos
        val t = (frameTimeNanos - startNanos).toDouble() / 1_000_000_000.0

        val model = hazardModelNode ?: return

        // Multi-harmonic thermal stretch & pulsation
        val stretchY = baseScale.y * (1.0f + (0.025f * sin(t * 5.2).toFloat()) +
                                              (0.015f * sin(t * 8.7 + 1.1).toFloat()))
        val squashXZ = baseScale.x * (1.0f - (0.012f * sin(t * 5.2).toFloat()) +
                                              (0.008f * cos(t * 7.3).toFloat()))
        model.scale = Float3(squashXZ, stretchY, squashXZ)

        // Subtle buoyant vertical pulsation & draft drift
        val buoyantY = (0.008f * sin(t * 4.6).toFloat()).coerceAtLeast(0.0f)
        val swayX = 0.005f * sin(t * 3.8).toFloat()
        val swayZ = 0.005f * cos(t * 3.3).toFloat()
        model.position = Float3(swayX, buoyantY, swayZ)
    }
}
