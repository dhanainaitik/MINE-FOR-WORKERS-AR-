package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

/**
 * Authentic Underground Coal Mine Environment.
 *
 * Implemented directly around the exact supplied model:
 * models/AR_Mine_Optimized.glb
 *
 * Sizing and alignment:
 * - Entrance portal threshold is at Z = +11.5m in model space.
 * - Floor level is at Y = -1.11m in model space.
 * - Model offset: position = Float3(0.0f, 1.11f, -11.5f)
 *   This places the entrance arch threshold exactly at the AR doorway (Z = 0.0m),
 *   aligns the mine floor with the real detected ground (Y = 0.0m),
 *   and extends the complete ~28-meter underground tunnel forward along the -Z axis.
 *
 * Enclosure (Requirement 11):
 * - Seamless natural rock outer enclosure and terminal rock bulkhead sealing the far end (Z = -28m)
 *   and framing the entrance portal (Z = 0.0m) to guarantee that the user's physical room,
 *   ceiling, and furniture never leak through mesh seams or open boundaries.
 */
class MineEnvironmentNode(
    engine: Engine,
    modelLoader: ModelLoader? = null,
    rockMaterial: MaterialInstance? = null,
    floorMaterial: MaterialInstance? = null,
    darkRockMaterial: MaterialInstance? = rockMaterial
) : Node(engine) {

    private val tag = "MineEnvironmentNode"

    init {
        // =========================================================================
        // 1. EXACT MINE ENVIRONMENT MODEL: AR_Mine_Optimized.glb
        // =========================================================================
        if (modelLoader != null) {
            try {
                val mineInstance = modelLoader.createModelInstance("models/AR_Mine_Optimized.glb")
                val mineNode = ModelNode(
                    modelInstance = mineInstance,
                    autoAnimate = false
                ).apply {
                    // Aligns model entrance arch (Z = +11.5m, Y = -1.11m) directly with the doorway anchor (0, 0, 0)
                    position = Float3(0.0f, 1.11f, -11.5f)
                    rotation = Float3(0.0f, 0.0f, 0.0f)
                    scale = Float3(1.0f, 1.0f, 1.0f)
                }
                addChildNode(mineNode)
                Log.d(tag, "Successfully loaded AR_Mine_Optimized.glb as the main mine environment")
            } catch (e: Exception) {
                Log.e(tag, "Error loading AR_Mine_Optimized.glb: ${e.message}", e)
            }
        }

        // =========================================================================
        // 2. SUBTERRANEAN ROCK ENCLOSURE & TERMINUS BULKHEAD
        // Guarantees zero background leakage from the real room (Requirement 11)
        // =========================================================================
        val mat = darkRockMaterial ?: rockMaterial

        // A. Continuous Sub-Floor Bedrock (covers Z = 0.2m to -30.0m)
        val bedrockSubFloor = CubeNode(
            engine,
            size = Float3(14.0f, 0.15f, 31.0f),
            materialInstance = mat
        ).apply {
            position = Float3(0.0f, -0.08f, -15.0f)
        }
        addChildNode(bedrockSubFloor)

        // B. Far Terminus Rock Bulkhead (seals the open end of the tunnel at Z = -28.5m)
        val terminalBulkhead = CubeNode(
            engine,
            size = Float3(14.0f, 8.0f, 1.5f),
            materialInstance = mat
        ).apply {
            position = Float3(0.0f, 3.5f, -28.5f)
        }
        addChildNode(terminalBulkhead)

        // Additional natural rock contours for the far bulkhead
        val rockPillar1 = CylinderNode(
            engine,
            radius = 1.2f,
            height = 5.5f,
            materialInstance = mat
        ).apply {
            position = Float3(-1.8f, 2.5f, -28.0f)
        }
        val rockPillar2 = CylinderNode(
            engine,
            radius = 1.4f,
            height = 5.5f,
            materialInstance = mat
        ).apply {
            position = Float3(1.9f, 2.5f, -27.8f)
        }
        addChildNode(rockPillar1)
        addChildNode(rockPillar2)

        // C. Lateral Rock Outer Shell (Left and Right Bedrock Flanks)
        val leftRockShell = CubeNode(
            engine,
            size = Float3(1.5f, 7.5f, 30.0f),
            materialInstance = mat
        ).apply {
            position = Float3(-5.2f, 3.5f, -14.5f)
        }
        val rightRockShell = CubeNode(
            engine,
            size = Float3(1.5f, 7.5f, 30.0f),
            materialInstance = mat
        ).apply {
            position = Float3(5.2f, 3.5f, -14.5f)
        }
        addChildNode(leftRockShell)
        addChildNode(rightRockShell)

        // D. Overburden Rock Vault (Upper Bedrock Ceiling Cap)
        val ceilingVault = CubeNode(
            engine,
            size = Float3(14.0f, 1.2f, 30.0f),
            materialInstance = mat
        ).apply {
            position = Float3(0.0f, 5.2f, -14.5f)
        }
        addChildNode(ceilingVault)

        // E. Portal Rock Face (Surrounds the doorway frame at Z = 0.0m)
        // Left portal rock wing
        val leftPortalFace = CubeNode(
            engine,
            size = Float3(4.0f, 5.0f, 0.6f),
            materialInstance = mat
        ).apply {
            position = Float3(-2.65f, 2.5f, 0.0f)
        }
        // Right portal rock wing
        val rightPortalFace = CubeNode(
            engine,
            size = Float3(4.0f, 5.0f, 0.6f),
            materialInstance = mat
        ).apply {
            position = Float3(2.65f, 2.5f, 0.0f)
        }
        // Top portal rock arch header
        val topPortalFace = CubeNode(
            engine,
            size = Float3(1.5f, 2.6f, 0.6f),
            materialInstance = mat
        ).apply {
            position = Float3(0.0f, 3.6f, 0.0f)
        }
        addChildNode(leftPortalFace)
        addChildNode(rightPortalFace)
        addChildNode(topPortalFace)

        // =========================================================================
        // 3. UNDERGROUND PRACTICAL MINE LIGHTING
        // Positioned along the tunnel path and illuminating the model's lantern points
        // =========================================================================
        val lights = listOf(
            // Portal entrance threshold
            Triple(Float3(0.0f, 2.2f, -0.6f), 45000f, 5.5f),
            // Approach to early equipment
            Triple(Float3(0.0f, 2.3f, -3.2f), 40000f, 6.0f),
            // Wall lantern station (Z = -5.6m in world coordinates)
            Triple(Float3(1.8f, 2.4f, -5.6f), 42000f, 6.0f),
            // Extinguisher location (Z = -8.0m)
            Triple(Float3(0.9f, 2.4f, -8.0f), 48000f, 6.5f),
            // Electrical fire training area (Z = -11.0m)
            Triple(Float3(0.7f, 2.5f, -11.0f), 50000f, 7.0f),
            // Wall lantern station deeper inside (Z = -12.8m)
            Triple(Float3(-1.8f, 2.4f, -12.8f), 40000f, 6.5f),
            // Mid tunnel chamber
            Triple(Float3(0.0f, 2.5f, -18.0f), 35000f, 7.5f),
            // Deep drift & terminal bulkhead
            Triple(Float3(0.0f, 2.5f, -25.0f), 30000f, 8.0f)
        )

        for ((pos, intensity, falloff) in lights) {
            try {
                val lightBuilder = LightManager.Builder(LightManager.Type.POINT)
                    .color(1.0f, 0.88f, 0.68f) // Warm incandescent miner lamp color
                    .intensity(intensity)
                    .falloff(falloff)
                val lightNode = LightNode(engine, builder = lightBuilder).apply {
                    position = pos
                }
                addChildNode(lightNode)
            } catch (e: Exception) {
                Log.w(tag, "Failed to create light at $pos: ${e.message}")
            }
        }
    }
}
