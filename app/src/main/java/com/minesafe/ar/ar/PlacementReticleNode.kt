package com.minesafe.ar.ar

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node

/**
 * AR Floor Placement Reticle matching Reference Image 2:
 * - Glowing cyan dashed boundary rectangle (1.4m x 1.1m)
 * - Corner angle brackets
 * - Footprint placement icons
 * - "TAP TO PLACE" callout geometry
 */
class PlacementReticleNode(
    engine: Engine,
    reticleMaterial: MaterialInstance?
) : Node(engine) {

    init {
        val width = 1.40f
        val length = 1.10f
        val lineThick = 0.025f
        val height = 0.005f // sits just above the floor plane

        val halfW = width / 2f
        val halfL = length / 2f

        // 1. Dashed Border Segments (North, South, East, West)
        // Top edge dashes
        val dashCountX = 7
        val dashLenX = (width - 0.2f) / dashCountX
        for (i in 0 until dashCountX) {
            if (i % 2 == 0) {
                val x = -halfW + 0.1f + (i * dashLenX) + (dashLenX / 2f)
                val dash = CubeNode(engine, size = Float3(dashLenX * 0.7f, height, lineThick), materialInstance = reticleMaterial).apply {
                    position = Float3(x, 0.005f, -halfL)
                }
                addChildNode(dash)
            }
        }

        // Bottom edge dashes
        for (i in 0 until dashCountX) {
            if (i % 2 == 0) {
                val x = -halfW + 0.1f + (i * dashLenX) + (dashLenX / 2f)
                val dash = CubeNode(engine, size = Float3(dashLenX * 0.7f, height, lineThick), materialInstance = reticleMaterial).apply {
                    position = Float3(x, 0.005f, halfL)
                }
                addChildNode(dash)
            }
        }

        // Left edge dashes
        val dashCountZ = 5
        val dashLenZ = (length - 0.2f) / dashCountZ
        for (i in 0 until dashCountZ) {
            if (i % 2 == 0) {
                val z = -halfL + 0.1f + (i * dashLenZ) + (dashLenZ / 2f)
                val dash = CubeNode(engine, size = Float3(lineThick, height, dashLenZ * 0.7f), materialInstance = reticleMaterial).apply {
                    position = Float3(-halfW, 0.005f, z)
                }
                addChildNode(dash)
            }
        }

        // Right edge dashes
        for (i in 0 until dashCountZ) {
            if (i % 2 == 0) {
                val z = -halfL + 0.1f + (i * dashLenZ) + (dashLenZ / 2f)
                val dash = CubeNode(engine, size = Float3(lineThick, height, dashLenZ * 0.7f), materialInstance = reticleMaterial).apply {
                    position = Float3(halfW, 0.005f, z)
                }
                addChildNode(dash)
            }
        }

        // 2. Corner Angle Brackets
        val cornerArmLen = 0.14f
        val corners = listOf(
            Pair(-halfW, -halfL),
            Pair(halfW, -halfL),
            Pair(-halfW, halfL),
            Pair(halfW, halfL)
        )
        for ((cx, cz) in corners) {
            val signX = if (cx < 0) 1f else -1f
            val signZ = if (cz < 0) 1f else -1f

            val armX = CubeNode(engine, size = Float3(cornerArmLen, height * 1.2f, lineThick * 1.4f), materialInstance = reticleMaterial).apply {
                position = Float3(cx + (signX * cornerArmLen / 2f), 0.006f, cz)
            }
            val armZ = CubeNode(engine, size = Float3(lineThick * 1.4f, height * 1.2f, cornerArmLen), materialInstance = reticleMaterial).apply {
                position = Float3(cx, 0.006f, cz + (signZ * cornerArmLen / 2f))
            }
            addChildNode(armX)
            addChildNode(armZ)
        }

        // 3. Footprint Placement Icons (Left foot & Right foot)
        // Left Foot
        val leftSole = CubeNode(engine, size = Float3(0.08f, height, 0.14f), materialInstance = reticleMaterial).apply {
            position = Float3(-0.09f, 0.007f, -0.04f)
            rotation = Float3(0f, -8f, 0f)
        }
        val leftHeel = CylinderNode(engine, radius = 0.035f, height = height, materialInstance = reticleMaterial).apply {
            position = Float3(-0.095f, 0.007f, 0.06f)
        }
        addChildNode(leftSole)
        addChildNode(leftHeel)

        // Right Foot
        val rightSole = CubeNode(engine, size = Float3(0.08f, height, 0.14f), materialInstance = reticleMaterial).apply {
            position = Float3(0.09f, 0.007f, -0.04f)
            rotation = Float3(0f, 8f, 0f)
        }
        val rightHeel = CylinderNode(engine, radius = 0.035f, height = height, materialInstance = reticleMaterial).apply {
            position = Float3(0.095f, 0.007f, 0.06f)
        }
        addChildNode(rightSole)
        addChildNode(rightHeel)

        // 4. "TAP TO PLACE" Callout Indicator Bar
        val textBar = CubeNode(engine, size = Float3(0.55f, height, 0.05f), materialInstance = reticleMaterial).apply {
            position = Float3(0.0f, 0.007f, 0.22f)
        }
        val textBarAccent = CubeNode(engine, size = Float3(0.35f, height, 0.02f), materialInstance = reticleMaterial).apply {
            position = Float3(0.0f, 0.008f, 0.28f)
        }
        addChildNode(textBar)
        addChildNode(textBarAccent)
    }
}
