package com.minesafe.ar.ar

import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Module 02: Chemical Hazard Node
 * - Industrial chemical containment drums (fluted steel, safety yellow with hazard bands)
 * - Pressurized pipe manifold with leaking flange
 * - Toxic yellowish-green vapor cloud (pulsing emissive spheres)
 * - Chemical pool on the rock floor
 * - 2.0-meter danger boundary perimeter ring on the ground
 * - Emergency isolation functionality
 */
class ChemicalHazardNode(
    engine: Engine,
    drumYellowMaterial: MaterialInstance?,
    hazardBandMaterial: MaterialInstance?,
    vaporMaterial: MaterialInstance?,
    puddleMaterial: MaterialInstance?,
    pipeMaterial: MaterialInstance?,
    perimeterMaterial: MaterialInstance?
) : Node(engine) {

    private val vaporCloudGroup = Node(engine)
    private var hazardLight: LightNode? = null
    var isControlled: Boolean = false
        private set

    init {
        // ==========================================
        // 1. CHEMICAL CONTAINMENT DRUMS (55-gallon style)
        // ==========================================
        val drumRadius = 0.28f
        val drumHeight = 0.88f

        // Primary leaking drum (upright)
        val drum1 = CylinderNode(engine, radius = drumRadius, height = drumHeight, materialInstance = drumYellowMaterial).apply {
            position = Float3(0.0f, drumHeight / 2f, 0.0f)
        }
        // Black hazard warning band around center
        val drum1Band = CylinderNode(engine, radius = drumRadius + 0.005f, height = 0.18f, materialInstance = hazardBandMaterial).apply {
            position = Float3(0.0f, drumHeight / 2f, 0.0f)
        }
        addChildNode(drum1)
        addChildNode(drum1Band)

        // Secondary drum (tilted slightly against the primary drum)
        val drum2 = CylinderNode(engine, radius = drumRadius, height = drumHeight, materialInstance = drumYellowMaterial).apply {
            position = Float3(0.55f, drumHeight / 2f - 0.04f, 0.12f)
            rotation = Float3(6f, 0f, -14f)
        }
        val drum2Band = CylinderNode(engine, radius = drumRadius + 0.005f, height = 0.18f, materialInstance = hazardBandMaterial).apply {
            position = Float3(0.55f, drumHeight / 2f - 0.04f, 0.12f)
            rotation = Float3(6f, 0f, -14f)
        }
        addChildNode(drum2)
        addChildNode(drum2Band)

        // ==========================================
        // 2. OVERHEAD PRESSURIZED PIPE & LEAKING FLANGE
        // ==========================================
        val pipeR = 0.05f
        val mainPipe = CylinderNode(engine, radius = pipeR, height = 1.4f, materialInstance = pipeMaterial).apply {
            position = Float3(0.0f, 1.25f, 0.0f)
            rotation = Float3(0f, 0f, 90f)
        }
        val pipeFlange = CylinderNode(engine, radius = pipeR + 0.04f, height = 0.06f, materialInstance = pipeMaterial).apply {
            position = Float3(0.05f, 1.25f, 0.0f)
            rotation = Float3(0f, 0f, 90f)
        }
        val leakNozzle = CylinderNode(engine, radius = 0.025f, height = 0.12f, materialInstance = pipeMaterial).apply {
            position = Float3(0.05f, 1.18f, 0.0f)
        }
        addChildNode(mainPipe)
        addChildNode(pipeFlange)
        addChildNode(leakNozzle)

        // ==========================================
        // 3. CORROSIVE LIQUID PUDDLE (On floor plane)
        // ==========================================
        val puddle = CylinderNode(engine, radius = 0.65f, height = 0.006f, materialInstance = puddleMaterial).apply {
            position = Float3(0.10f, 0.006f, 0.05f)
            scale = Float3(1.3f, 1.0f, 0.9f)
        }
        addChildNode(puddle)

        // ==========================================
        // 4. TOXIC VAPOR CLOUD (Pulsing green/yellow mist)
        // ==========================================
        addChildNode(vaporCloudGroup)

        val v1 = SphereNode(engine, radius = 0.26f, materialInstance = vaporMaterial).apply {
            position = Float3(0.05f, 1.05f, 0.02f)
            scale = Float3(1.2f, 0.9f, 1.1f)
        }
        val v2 = SphereNode(engine, radius = 0.38f, materialInstance = vaporMaterial).apply {
            position = Float3(0.12f, 1.35f, 0.08f)
            scale = Float3(1.4f, 1.1f, 1.3f)
        }
        val v3 = SphereNode(engine, radius = 0.48f, materialInstance = vaporMaterial).apply {
            position = Float3(-0.10f, 1.65f, -0.05f)
            scale = Float3(1.6f, 1.3f, 1.5f)
        }
        vaporCloudGroup.addChildNode(v1)
        vaporCloudGroup.addChildNode(v2)
        vaporCloudGroup.addChildNode(v3)

        // Chemical Hazard Point Light (Toxic Yellow-Green glow)
        try {
            val lightBuilder = LightManager.Builder(LightManager.Type.POINT)
                .color(0.65f, 1.0f, 0.15f)
                .intensity(75000f)
                .falloff(7.5f)
            val light = LightNode(engine, builder = lightBuilder).apply {
                position = Float3(0.05f, 1.25f, 0.05f)
            }
            hazardLight = light
            addChildNode(light)
        } catch (_: Exception) {}

        // ==========================================
        // 5. 2.0-METER DANGER PERIMETER RING
        // Dashed warning circle on the ground
        // ==========================================
        val radius2m = 2.0f
        val segCount = 18
        for (i in 0 until segCount) {
            val theta = (i * 2.0 * PI / segCount).toFloat()
            val px = sin(theta) * radius2m
            val pz = cos(theta) * radius2m
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
        if (controlled) {
            vaporCloudGroup.isVisible = false
            hazardLight?.destroy()
            hazardLight = null
        }
    }
}
