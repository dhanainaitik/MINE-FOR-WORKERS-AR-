package com.minesafe.ar.ar

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node

/**
 * Real-world scale industrial mine black doorway frame (Working Reference).
 * Height: 2.13 meters (7 feet)
 * Inner Opening Width: 1.07 meters (3.5 feet)
 *
 * Placed immediately and solidly on the ARCore floor anchor upon user tap.
 */
class DoorwayNode(
    engine: Engine,
    frameMaterial: MaterialInstance? = null,
    rockMaterial: MaterialInstance? = null,
    woodMaterial: MaterialInstance? = null,
    metalMaterial: MaterialInstance? = null,
    lampEmissiveMaterial: MaterialInstance? = null,
    signMaterial: MaterialInstance? = null
) : Node(engine) {

    init {
        val mat = frameMaterial ?: metalMaterial ?: woodMaterial

        // Left frame post (height 2.13m, width 0.12m, depth 0.2m)
        val leftFrame = CubeNode(engine, size = Float3(0.12f, 2.13f, 0.2f), materialInstance = mat).apply {
            position = Float3(-0.595f, 1.065f, 0.0f)
        }

        // Right frame post
        val rightFrame = CubeNode(engine, size = Float3(0.12f, 2.13f, 0.2f), materialInstance = mat).apply {
            position = Float3(0.595f, 1.065f, 0.0f)
        }

        // Top frame beam
        val topFrame = CubeNode(engine, size = Float3(1.31f, 0.12f, 0.2f), materialInstance = mat).apply {
            position = Float3(0.0f, 2.19f, 0.0f)
        }

        // Floor threshold bar
        val threshold = CubeNode(engine, size = Float3(1.31f, 0.02f, 0.2f), materialInstance = mat).apply {
            position = Float3(0.0f, 0.01f, 0.0f)
        }

        addChildNode(leftFrame)
        addChildNode(rightFrame)
        addChildNode(topFrame)
        addChildNode(threshold)
    }
}
