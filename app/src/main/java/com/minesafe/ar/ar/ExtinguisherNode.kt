package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Realistic Industrial Fire Extinguisher Node:
 * - Loads authentic 3D model: models/fire_extinguisher.glb
 * - Scaled to human proportions (~0.64m height, ~0.18m cylinder diameter)
 * - Base sits directly on the mine floor (Y = 0.00m) with zero gap
 * - Positioned on the right side of the walking path before the electrical fire
 * - Strictly module-scoped to Module 01 (Electrical Fire Safety); never instantiated in Module 02
 */
class ExtinguisherNode(
    engine: Engine,
    modelLoader: ModelLoader
) : Node(engine) {

    private val tag = "ExtinguisherNode"
    var modelNode: ModelNode? = null
        private set

    // Interactive anchors / handles for training interaction
    val safetyPin: Node = Node(engine)
    val nozzle: Node = Node(engine)

    var isPinRemoved: Boolean = false
        private set
    var isNozzleReady: Boolean = false
        private set

    init {
        try {
            val modelInstance = modelLoader.createModelInstance("models/fire_extinguisher.glb")
            // fire_extinguisher.glb bounds: Y in [-1.0, 1.0] (total height = 2.0 units).
            // With scale 0.32f, extinguisher height is 0.64m (standard industrial 10lb extinguisher).
            // Base of model is at local Y = -1.0 * 0.32 = -0.32m.
            // Offsetting ModelNode by +0.32m places the base flush at Y = 0.00m on the mine floor.
            val mNode = ModelNode(
                    modelInstance = modelInstance,
                    autoAnimate = false
                ).apply {
                    position = Float3(0.0f, 0.32f, 0.0f)
                    // Stand upright with front label, gauge, and handle oriented facing the approaching worker from the left
                    rotation = Float3(0.0f, 25.0f, 0.0f)
                    scale = Float3(0.32f, 0.32f, 0.32f)
                }
                addChildNode(mNode)
                modelNode = mNode
                Log.i(tag, "fire_extinguisher.glb successfully loaded and placed on mine floor")
        } catch (e: Exception) {
            Log.e(tag, "Error loading fire_extinguisher.glb: ${e.message}", e)
        }

        addChildNode(safetyPin)
        addChildNode(nozzle)
    }

    fun removeSafetyPin() {
        isPinRemoved = true
    }

    fun openNozzle() {
        isNozzleReady = true
    }

    fun setDischarging(active: Boolean) {
        // Reserved for subsequent particle discharge implementation
    }

    override fun destroy() {
        super.destroy()
    }
}
