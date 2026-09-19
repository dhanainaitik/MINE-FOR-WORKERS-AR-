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

/**
 * Realistic Electrical Substation Fire Hazard:
 * - Industrial metal control cabinet with vented access panel and charred wiring
 * - Multi-tiered flickering flame tongues (bright yellow inner core + orange outer flame)
 * - Rising dark smoke puffs
 * - Dynamic warm orange/red point light
 */
class FireNode(
    engine: Engine,
    cabinetMaterial: MaterialInstance?,
    fireCoreMaterial: MaterialInstance?,
    fireOuterMaterial: MaterialInstance?,
    smokeMaterial: MaterialInstance?,
    warningSignMaterial: MaterialInstance?
) : Node(engine) {

    private val flameGroup = Node(engine)
    private val smokeGroup = Node(engine)
    private var fireLight: LightNode? = null
    private var isExtinguished = false

    init {
        // ==========================================
        // 1. ELECTRICAL SUBSTATION CABINET
        // ==========================================
        val cabWidth = 0.75f
        val cabHeight = 1.15f
        val cabDepth = 0.38f

        // Main steel cabinet body
        val cabinet = CubeNode(engine, size = Float3(cabWidth, cabHeight, cabDepth), materialInstance = cabinetMaterial).apply {
            position = Float3(0.0f, cabHeight / 2f, 0.0f)
        }
        addChildNode(cabinet)

        // Concrete equipment mounting plinth
        val plinth = CubeNode(engine, size = Float3(cabWidth + 0.15f, 0.12f, cabDepth + 0.15f), materialInstance = cabinetMaterial).apply {
            position = Float3(0.0f, 0.06f, 0.0f)
        }
        addChildNode(plinth)

        // Electrical junction box & conduit pipes entering the top
        val conduit1 = CylinderNode(engine, radius = 0.035f, height = 0.6f, materialInstance = cabinetMaterial).apply {
            position = Float3(-0.20f, cabHeight + 0.3f, 0.0f)
        }
        val conduit2 = CylinderNode(engine, radius = 0.035f, height = 0.6f, materialInstance = cabinetMaterial).apply {
            position = Float3(0.20f, cabHeight + 0.3f, 0.0f)
        }
        addChildNode(conduit1)
        addChildNode(conduit2)

        // High Voltage Warning Plate
        val warningSign = CubeNode(engine, size = Float3(0.24f, 0.18f, 0.02f), materialInstance = warningSignMaterial).apply {
            position = Float3(0.0f, cabHeight * 0.82f, (cabDepth / 2f) + 0.015f)
        }
        addChildNode(warningSign)

        // Open/charred access door exposing internal components
        val openDoor = CubeNode(engine, size = Float3(0.35f, 0.65f, 0.02f), materialInstance = cabinetMaterial).apply {
            position = Float3(-0.35f, 0.55f, 0.22f)
            rotation = Float3(0f, 40f, 0f)
        }
        addChildNode(openDoor)

        // ==========================================
        // 2. FLAME SYSTEM (Inner Core + Outer Tongues)
        // ==========================================
        addChildNode(flameGroup)

        // Inner Core (Intense yellow/white heat)
        val coreFlame1 = SphereNode(engine, radius = 0.18f, materialInstance = fireCoreMaterial).apply {
            position = Float3(0.0f, 0.55f, 0.10f)
            scale = Float3(0.8f, 1.4f, 0.8f)
        }
        val coreFlame2 = SphereNode(engine, radius = 0.14f, materialInstance = fireCoreMaterial).apply {
            position = Float3(0.12f, 0.65f, 0.12f)
            scale = Float3(0.7f, 1.2f, 0.7f)
        }
        flameGroup.addChildNode(coreFlame1)
        flameGroup.addChildNode(coreFlame2)

        // Outer Flame Tongues (Vibrant orange/red fire)
        val outerFlame1 = SphereNode(engine, radius = 0.25f, materialInstance = fireOuterMaterial).apply {
            position = Float3(0.0f, 0.62f, 0.08f)
            scale = Float3(1.0f, 1.6f, 1.0f)
        }
        val outerFlame2 = SphereNode(engine, radius = 0.20f, materialInstance = fireOuterMaterial).apply {
            position = Float3(-0.14f, 0.70f, 0.06f)
            scale = Float3(0.8f, 1.3f, 0.8f)
        }
        val outerFlame3 = SphereNode(engine, radius = 0.22f, materialInstance = fireOuterMaterial).apply {
            position = Float3(0.14f, 0.78f, 0.08f)
            scale = Float3(0.7f, 1.5f, 0.7f)
        }
        flameGroup.addChildNode(outerFlame1)
        flameGroup.addChildNode(outerFlame2)
        flameGroup.addChildNode(outerFlame3)

        // ==========================================
        // 3. SMOKE PUFFS (Dark rising clouds)
        // ==========================================
        addChildNode(smokeGroup)
        val smokePuff1 = SphereNode(engine, radius = 0.22f, materialInstance = smokeMaterial).apply {
            position = Float3(0.05f, 1.25f, 0.05f)
        }
        val smokePuff2 = SphereNode(engine, radius = 0.28f, materialInstance = smokeMaterial).apply {
            position = Float3(-0.08f, 1.55f, 0.02f)
        }
        val smokePuff3 = SphereNode(engine, radius = 0.35f, materialInstance = smokeMaterial).apply {
            position = Float3(0.10f, 1.90f, -0.04f)
        }
        smokeGroup.addChildNode(smokePuff1)
        smokeGroup.addChildNode(smokePuff2)
        smokeGroup.addChildNode(smokePuff3)

        // ==========================================
        // 4. DYNAMIC FIRE LIGHT (Orange/Red Flicker)
        // ==========================================
        try {
            val lightBuilder = LightManager.Builder(LightManager.Type.POINT)
                .color(1.0f, 0.50f, 0.12f)
                .intensity(85000f)
                .falloff(8.0f)
            val light = LightNode(engine, builder = lightBuilder).apply {
                position = Float3(0.0f, 0.75f, 0.15f)
            }
            fireLight = light
            addChildNode(light)
        } catch (_: Exception) {}
    }

    fun setFireScale(scale: Float) {
        val s = scale.coerceIn(0.0f, 1.0f)
        flameGroup.scale = Float3(s, s, s)
        smokeGroup.scale = Float3(s, s, s)

        if (s <= 0.05f && !isExtinguished) {
            isExtinguished = true
            flameGroup.isVisible = false
            smokeGroup.isVisible = false
            fireLight?.destroy()
            fireLight = null
        } else if (fireLight != null) {
            fireLight?.intensity = 85000f * s
        }
    }
}
