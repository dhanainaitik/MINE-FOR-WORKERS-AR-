package com.minesafe.ar.ar

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

/**
 * Realistic Industrial Fire Extinguisher Node:
 * - Steel pressure cylinder (Class ABC/Dry Chemical) with safety red finish
 * - Valve head assembly with brass neck
 * - Operating squeeze lever and fixed carrying handle
 * - Interactive safety pull-pin ring
 * - Circular pressure gauge with green inspection zone
 * - Reinforced flexible rubber hose with discharge horn/nozzle (interactive)
 * - Discharge powder/gas cone effect when activated
 */
class ExtinguisherNode(
    engine: Engine,
    redBodyMaterial: MaterialInstance?,
    metalMaterial: MaterialInstance?,
    brassMaterial: MaterialInstance?,
    rubberMaterial: MaterialInstance?,
    sprayMaterial: MaterialInstance?,
    gaugeGreenMaterial: MaterialInstance?
) : Node(engine) {

    val safetyPin: CubeNode
    val nozzle: CubeNode
    private val dischargeSprayGroup = Node(engine)
    private var spray1: SphereNode? = null
    private var spray2: SphereNode? = null
    private var spray3: SphereNode? = null
    var isPinRemoved: Boolean = false
        private set
    var isNozzleReady: Boolean = false
        private set

    init {
        val cylRadius = 0.095f
        val cylHeight = 0.48f

        // 1. Cylinder Base & Foot Ring
        val footRing = CylinderNode(engine, radius = cylRadius + 0.008f, height = 0.04f, materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.02f, 0.0f)
        }
        addChildNode(footRing)

        // 2. Main Red Cylinder Tank
        val tank = CylinderNode(engine, radius = cylRadius, height = cylHeight, materialInstance = redBodyMaterial).apply {
            position = Float3(0.0f, 0.28f, 0.0f)
        }
        addChildNode(tank)

        // Inspection / Instruction Label Band
        val labelBand = CylinderNode(engine, radius = cylRadius + 0.002f, height = 0.16f, materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.28f, 0.0f)
        }
        addChildNode(labelBand)

        // 3. Top Dome & Neck Collar
        val dome = SphereNode(engine, radius = cylRadius, materialInstance = redBodyMaterial).apply {
            position = Float3(0.0f, 0.52f, 0.0f)
            scale = Float3(1.0f, 0.5f, 1.0f)
        }
        val brassNeck = CylinderNode(engine, radius = 0.035f, height = 0.08f, materialInstance = brassMaterial).apply {
            position = Float3(0.0f, 0.56f, 0.0f)
        }
        addChildNode(dome)
        addChildNode(brassNeck)

        // 4. Valve Head, Fixed Handle & Operating Squeeze Lever
        val valveBlock = CubeNode(engine, size = Float3(0.06f, 0.06f, 0.09f), materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.61f, 0.0f)
        }
        // Fixed carrying handle (lower)
        val carryHandle = CubeNode(engine, size = Float3(0.035f, 0.02f, 0.18f), materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.60f, -0.07f)
            rotation = Float3(-12f, 0f, 0f)
        }
        // Squeeze trigger lever (upper)
        val squeezeLever = CubeNode(engine, size = Float3(0.032f, 0.018f, 0.19f), materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.65f, -0.06f)
            rotation = Float3(16f, 0f, 0f)
        }
        addChildNode(valveBlock)
        addChildNode(carryHandle)
        addChildNode(squeezeLever)

        // 5. Interactive Pull-Pin (Bright metallic safety pin with ring)
        safetyPin = CubeNode(engine, size = Float3(0.12f, 0.025f, 0.025f), materialInstance = brassMaterial).apply {
            position = Float3(0.05f, 0.63f, 0.01f)
        }
        val pinRing = CylinderNode(engine, radius = 0.035f, height = 0.015f, materialInstance = brassMaterial).apply {
            position = Float3(0.12f, 0.63f, 0.01f)
            rotation = Float3(0f, 0f, 90f)
        }
        safetyPin.addChildNode(pinRing)
        addChildNode(safetyPin)

        // 6. Pressure Gauge
        val gaugeDial = CylinderNode(engine, radius = 0.024f, height = 0.02f, materialInstance = metalMaterial).apply {
            position = Float3(-0.045f, 0.61f, 0.0f)
            rotation = Float3(0f, 0f, 90f)
        }
        val gaugeFace = CylinderNode(engine, radius = 0.020f, height = 0.005f, materialInstance = gaugeGreenMaterial).apply {
            position = Float3(-0.056f, 0.61f, 0.0f)
            rotation = Float3(0f, 0f, 90f)
        }
        addChildNode(gaugeDial)
        addChildNode(gaugeFace)

        // 7. Discharge Hose & Flared Horn Nozzle (Interactive)
        val hoseBase = CylinderNode(engine, radius = 0.018f, height = 0.12f, materialInstance = rubberMaterial).apply {
            position = Float3(0.0f, 0.58f, 0.06f)
            rotation = Float3(50f, 0f, 0f)
        }
        val hoseCurve = CylinderNode(engine, radius = 0.018f, height = 0.35f, materialInstance = rubberMaterial).apply {
            position = Float3(0.08f, 0.38f, 0.08f)
            rotation = Float3(-35f, 15f, 0f)
        }
        addChildNode(hoseBase)
        addChildNode(hoseCurve)

        // Interactive Nozzle / Discharge Horn
        nozzle = CubeNode(engine, size = Float3(0.07f, 0.07f, 0.20f), materialInstance = rubberMaterial).apply {
            position = Float3(0.06f, 0.50f, 0.16f)
            rotation = Float3(-25f, 0f, 0f)
        }
        val nozzleTip = CylinderNode(engine, radius = 0.045f, height = 0.08f, materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 0.0f, 0.10f)
            rotation = Float3(90f, 0f, 0f)
        }
        nozzle.addChildNode(nozzleTip)
        addChildNode(nozzle)

        // 8. Discharge Spray Cone Cloud (Explicitly hidden until discharged)
        dischargeSprayGroup.isVisible = false
        addChildNode(dischargeSprayGroup)

        val s1 = SphereNode(engine, radius = 0.14f, materialInstance = sprayMaterial).apply {
            position = Float3(0.06f, 0.48f, 0.40f)
            isVisible = false
        }
        val s2 = SphereNode(engine, radius = 0.28f, materialInstance = sprayMaterial).apply {
            position = Float3(0.06f, 0.45f, 0.75f)
            scale = Float3(1.2f, 1.2f, 1.6f)
            isVisible = false
        }
        val s3 = SphereNode(engine, radius = 0.42f, materialInstance = sprayMaterial).apply {
            position = Float3(0.06f, 0.42f, 1.20f)
            scale = Float3(1.5f, 1.4f, 2.0f)
            isVisible = false
        }
        spray1 = s1
        spray2 = s2
        spray3 = s3
        dischargeSprayGroup.addChildNode(s1)
        dischargeSprayGroup.addChildNode(s2)
        dischargeSprayGroup.addChildNode(s3)
    }

    fun removeSafetyPin() {
        isPinRemoved = true
        safetyPin.isVisible = false
    }

    fun openNozzle() {
        isNozzleReady = true
        nozzle.rotation = Float3(-10f, 0f, 0f)
    }

    fun setDischarging(active: Boolean) {
        dischargeSprayGroup.isVisible = active
        spray1?.isVisible = active
        spray2?.isVisible = active
        spray3?.isVisible = active
    }
}
