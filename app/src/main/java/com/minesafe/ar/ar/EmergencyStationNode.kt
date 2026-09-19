package com.minesafe.ar.ar

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node

/**
 * Wall-Mounted Emergency Response & Isolation Station (for Module 02):
 * - High-visibility green station board with safety cross
 * - Interactive emergency isolation valve wheel & shutoff lever
 * - Spill response kit cabinet & emergency eyewash station
 */
class EmergencyStationNode(
    engine: Engine,
    boardMaterial: MaterialInstance?,
    cabinetMaterial: MaterialInstance?,
    valveMaterial: MaterialInstance?,
    metalMaterial: MaterialInstance?,
    whiteCrossMaterial: MaterialInstance?
) : Node(engine) {

    val isolationValve: CylinderNode
    val isolationLever: CubeNode
    var isValveActivated: Boolean = false
        private set

    init {
        val boardW = 0.85f
        val boardH = 1.30f
        val boardD = 0.04f

        // 1. Station Backboard (Industrial Green)
        val board = CubeNode(engine, size = Float3(boardW, boardH, boardD), materialInstance = boardMaterial).apply {
            position = Float3(0.0f, 1.35f, 0.0f)
        }
        addChildNode(board)

        // Safety White Cross Header
        val crossH = CubeNode(engine, size = Float3(0.24f, 0.07f, 0.015f), materialInstance = whiteCrossMaterial).apply {
            position = Float3(0.0f, 1.82f, 0.025f)
        }
        val crossV = CubeNode(engine, size = Float3(0.07f, 0.24f, 0.015f), materialInstance = whiteCrossMaterial).apply {
            position = Float3(0.0f, 1.82f, 0.025f)
        }
        addChildNode(crossH)
        addChildNode(crossV)

        // 2. Spill Response Cabinet (Lower half)
        val cab = CubeNode(engine, size = Float3(0.55f, 0.45f, 0.22f), materialInstance = cabinetMaterial).apply {
            position = Float3(0.0f, 1.05f, 0.11f)
        }
        addChildNode(cab)

        // 3. Emergency Isolation Valve Wheel (Interactive)
        val valveStem = CylinderNode(engine, radius = 0.03f, height = 0.16f, materialInstance = metalMaterial).apply {
            position = Float3(0.0f, 1.48f, 0.08f)
            rotation = Float3(90f, 0f, 0f)
        }
        isolationValve = CylinderNode(engine, radius = 0.14f, height = 0.04f, materialInstance = valveMaterial).apply {
            position = Float3(0.0f, 1.48f, 0.18f)
            rotation = Float3(90f, 0f, 0f)
        }
        addChildNode(valveStem)
        addChildNode(isolationValve)

        // Emergency Shutoff Lever
        isolationLever = CubeNode(engine, size = Float3(0.04f, 0.25f, 0.03f), materialInstance = valveMaterial).apply {
            position = Float3(0.26f, 1.48f, 0.08f)
            rotation = Float3(0f, 0f, -25f)
        }
        addChildNode(isolationLever)

        // 4. Emergency Eyewash Basin
        val eyewashBowl = CylinderNode(engine, radius = 0.11f, height = 0.08f, materialInstance = metalMaterial).apply {
            position = Float3(-0.25f, 1.30f, 0.14f)
        }
        addChildNode(eyewashBowl)
    }

    fun activateValve() {
        isValveActivated = true
        isolationValve.rotation = Float3(90f, 90f, 0f)
        isolationLever.rotation = Float3(0f, 0f, 35f)
    }
}
