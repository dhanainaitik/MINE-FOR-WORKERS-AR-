package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Industrial Electrical Box Node:
 * - Loads authentic industrial high-voltage breaker/switch box model: industrial_electrical_box (1).glb
 * - Positioned directly above the animated fire hazard in Module 01 (Electrical Fire Safety)
 * - Scaled to realistic industrial proportions (~0.65m height, ~0.45m width)
 * - Facing towards the approaching worker and railway so the open door and interior are visible
 * - Strictly module-scoped to Module 01: never instantiated or displayed in Module 02
 */
class ElectricalBoxNode(
    engine: Engine,
    modelLoader: ModelLoader
) : Node(engine) {

    private val tag = "ElectricalBoxNode"
    var boxModelNode: ModelNode? = null
        private set

    init {
        try {
            val boxInstance = try {
                modelLoader.createModelInstance("models/industrial_electrical_box (1).glb")
            } catch (e: Exception) {
                Log.w(tag, "Failed to load models/industrial_electrical_box (1).glb (${e.message}), trying industrial_electrical_box.glb")
                modelLoader.createModelInstance("models/industrial_electrical_box.glb")
            } ?: modelLoader.createModelInstance("models/industrial_electrical_box.glb")

            val modelNode = ModelNode(
                modelInstance = boxInstance,
                autoAnimate = false
            ).apply {
                position = Float3(0.0f, 0.0f, 0.0f)
                rotation = Float3(0.0f, 0.0f, 0.0f)
                scale = Float3(1.25f, 1.25f, 1.25f)
            }

            addChildNode(modelNode)
            boxModelNode = modelNode
            Log.i(tag, "ElectricalBoxNode successfully initialized")
        } catch (e: Exception) {
            Log.e(tag, "Error loading electrical box model: ${e.message}", e)
        }
    }

    override fun destroy() {
        super.destroy()
    }
}
