package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Module 02: Chemical Safety Kit Node
 * - Loads authentic 3D model: models/chemical_kit.glb (original supplied chemical-kit-model)
 * - GM-1955 Gas Mask / Chemical Respirator Kit (facepiece, protective lenses, breathing tube, filtration canisters)
 * - Scaled to realistic human equipment proportions (~1.33m height, ~0.75m width)
 * - Base of canisters sits directly on the mine floor (Y = 0.00m) with zero gap / not underground
 * - Centered on its local origin and rotated to face the approaching worker
 * - Positioned on the LEFT side of the worker's virtual walking path before the chemical hazard
 * - Strictly module-scoped to Module 02 (Chemical Hazard Response); never touches Module 01
 */
class ChemicalKitNode(
    engine: Engine,
    modelLoader: ModelLoader
) : Node(engine) {

    private val tag = "ChemicalKitNode"
    var modelNode: ModelNode? = null
        private set

    // Interactive handle / touch target for training interaction
    val kitInteractiveTarget: Node = Node(engine)

    var isKitEquipped: Boolean = false
        private set

    init {
        try {
            val modelInstance = modelLoader.createModelInstance("models/chemical_kit.glb")
            // chemical_kit.glb bounds in transformed GLB coordinates:
            // Y in [-89.909, 21.230] -> total height = 111.139 units.
            // X center = -2.055 units, Z center = -45.421 units.
            // With scale s = 0.012f:
            // Total height = 1.334m (realistic human equipment proportions).
            // Canisters at bottom: 0.43m tall, 0.35m wide.
            // Mask face: 0.59m tall, 0.45m wide.
            val scaleFactor = 0.012f

            // Inner ModelNode: Scales the model and shifts it so:
            // - Horizontal center (X = -2.055, Z = -45.421) is centered at (0, 0)
            // - Vertical bottom (Y = -89.909) is grounded flush at Y = 0.00m
            val mNode = ModelNode(
                modelInstance = modelInstance,
                autoAnimate = false
            ).apply {
                position = Float3(2.055f * scaleFactor, 89.909f * scaleFactor, 45.421f * scaleFactor)
                scale = Float3(scaleFactor, scaleFactor, scaleFactor)
            }

            // Pivot wrapper: Rotates the centered kit cleanly in place around its vertical axis
            // The mask face in GLB space points along +X (+88 deg).
            // Rotating by -70 deg turns the face and goggles toward +Z and slightly inward
            // so they directly face the worker approaching down the tunnel from the entrance.
            val pivotNode = Node(engine).apply {
                rotation = Float3(0.0f, -70.0f, 0.0f)
            }
            pivotNode.addChildNode(mNode)
            addChildNode(pivotNode)

            modelNode = mNode
            Log.i(tag, "chemical_kit.glb successfully loaded, centered, and grounded at Y = 0.00m (Module 02)")
        } catch (e: Exception) {
            Log.e(tag, "Error loading chemical_kit.glb: ${e.message}", e)
        }

        // Interactive touch target at chest height (~0.7m)
        kitInteractiveTarget.position = Float3(0.0f, 0.70f, 0.0f)
        addChildNode(kitInteractiveTarget)
    }

    fun equipKit() {
        isKitEquipped = true
    }

    override fun destroy() {
        super.destroy()
    }
}
