package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Module 02: Chemical Hazard Node
 * - Industrial chemical tank: models/chemical_tank.glb
 * - Industrial chemical containment drums: models/metal_barrel.glb
 * - Both assets are grounded at Y = 0.00m (sitting naturally on the floor)
 * - Orientation: Front/labels face the approaching worker coming down the tunnel (+Z)
 * - Realistic spacing beside the railway, completely clear of tracks and walls
 * - Clean, realistic 3D assets with zero unwanted green vapor bubbles / gas clouds / puddles
 * - 2.0-meter danger boundary perimeter ring on the ground
 * - Strictly scoped to Module 02; never touches Module 01
 */
class ChemicalHazardNode(
    engine: Engine,
    modelLoader: ModelLoader,
    vaporMaterial: MaterialInstance? = null,
    puddleMaterial: MaterialInstance? = null,
    perimeterMaterial: MaterialInstance? = null
) : Node(engine) {

    private val tag = "ChemicalHazardNode"
    var tankModelNode: ModelNode? = null
        private set
    var barrelModelNode: ModelNode? = null
        private set
    var secondaryBarrelModelNode: ModelNode? = null
        private set

    var isControlled: Boolean = false
        private set

    init {
        // ==========================================
        // 1. INDUSTRIAL CHEMICAL TANK (chemical_tank.glb)
        // ==========================================
        try {
            val tankInstance = modelLoader.createModelInstance("models/chemical_tank.glb")
            // chemical_tank.glb transformed bounds:
            // Width X in [-1.43, 1.31] (~2.74m)
            // Height Y in [0.29, 5.97] (~5.69m)
            // Depth Z in [-3.09, 3.13] (~6.22m)
            // Scale: 0.35f gives height ~1.99m (well below 2.91m mine ceiling), width ~0.96m, length ~2.18m.
            // Support legs min Y = 0.286m * 0.35 = 0.100m.
            // Offset Y = -0.100m places the feet solidly on the mine floor at Y = 0.00m.
            // Tank positioned at X = 1.35m, Z = -14.20m (right side of track, clear of ties at X <= 0.65m and wall at X >= 2.10m).
            // Rotation: oriented so front ladder, cage, and inspection platform face the approaching worker (+Z).
            val tank = ModelNode(
                modelInstance = tankInstance,
                autoAnimate = false
            ).apply {
                position = Float3(1.35f, -0.100f, -14.20f)
                rotation = Float3(0.0f, -5.0f, 0.0f)
                scale = Float3(0.35f, 0.35f, 0.35f)
            }
            addChildNode(tank)
            tankModelNode = tank
            Log.i(tag, "chemical_tank.glb successfully loaded and grounded at Y = 0.00m")
        } catch (e: Exception) {
            Log.e(tag, "Error loading chemical_tank.glb: ${e.message}", e)
        }

        // ==========================================
        // 2. INDUSTRIAL CHEMICAL DRUMS (metal_barrel.glb)
        // ==========================================
        try {
            // Primary Barrel: placed beside the tank on the mine floor
            // metal_barrel.glb transformed bounds:
            // Diameter ~36.8, Height ~58.64 (standard 55-gallon drum ratio)
            // Scale 0.015f gives height = 0.88m, diameter = 0.55m.
            // Base offset: -0.011m places bottom rim flush with mine floor at Y = 0.00m.
            // Positioned at X = 1.05m, Z = -13.00m (beside tank, completely clear of railway ties at X <= 0.65m).
            // Rotation: rotated by +15.0 deg around Y so the yellow hazard diamond warning label
            // directly faces the worker approaching down the tunnel from +Z.
            val barrelInstance1 = modelLoader.createModelInstance("models/metal_barrel.glb")
            val barrel1 = ModelNode(
                modelInstance = barrelInstance1,
                autoAnimate = false
            ).apply {
                position = Float3(1.05f, -0.011f, -13.00f)
                rotation = Float3(0.0f, 15.0f, 0.0f)
                scale = Float3(0.015f, 0.015f, 0.015f)
            }
            addChildNode(barrel1)
            barrelModelNode = barrel1

            // Secondary Barrel: grouped beside primary drum against right shoulder
            // Positioned at X = 1.58m, Z = -13.10m, rotation +20.0 deg so label faces approaching worker
            val barrelInstance2 = modelLoader.createModelInstance("models/metal_barrel.glb")
            val barrel2 = ModelNode(
                modelInstance = barrelInstance2,
                autoAnimate = false
            ).apply {
                position = Float3(1.58f, -0.011f, -13.10f)
                rotation = Float3(0.0f, 20.0f, 0.0f)
                scale = Float3(0.015f, 0.015f, 0.015f)
            }
            addChildNode(barrel2)
            secondaryBarrelModelNode = barrel2
            Log.i(tag, "metal_barrel.glb successfully loaded, oriented, and grounded at Y = 0.00m")
        } catch (e: Exception) {
            Log.e(tag, "Error loading metal_barrel.glb: ${e.message}", e)
        }

        // ==========================================
        // 3. 2.0-METER DANGER PERIMETER RING
        // Dashed warning circle on the ground around the hazard area
        // ==========================================
        val radius2m = 2.0f
        val segCount = 18
        val centerX = 1.30f
        val centerZ = -13.60f
        for (i in 0 until segCount) {
            val theta = (i * 2.0 * PI / segCount).toFloat()
            val px = sin(theta) * radius2m + centerX
            val pz = cos(theta) * radius2m + centerZ
            val rotY = Math.toDegrees(theta.toDouble()).toFloat()

            val seg = CubeNode(engine, size = Float3(0.25f, 0.005f, 0.04f), materialInstance = perimeterMaterial).apply {
                position = Float3(px, 0.006f, pz)
                rotation = Float3(0f, rotY + 90f, 0f)
            }
            addChildNode(seg)
        }
    }

    fun setHazardControlled(controlled: Boolean) {
        isControlled = controlled
    }

    override fun destroy() {
        super.destroy()
    }
}
