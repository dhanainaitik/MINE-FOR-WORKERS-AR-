package com.minesafe.ar.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.PlaneNode

/**
 * Authentic Underground Coal Mine Evacuation Directional Sign Board:
 *
 * Displays the original weathered industrial "TURN RIGHT" hazard safety sign:
 * - Mounted securely at the junction before the first left curve.
 * - Positioned at eye level (Y = 1.40m) on a realistic heavy mine timber upright support beam.
 * - Angled toward the player's approach direction so the text is immediately readable.
 * - Bold right-pointing arrow points directly toward the actual right evacuation tunnel.
 * - Features heavy metal backing plate, corner mounting hex bolts, and overhead illumination.
 * - Leaves the walkway 100% unobstructed for smooth player navigation.
 */
class SignBoardNode(
    engine: Engine,
    materialLoader: MaterialLoader,
    context: Context,
    timberMaterial: MaterialInstance? = null,
    metalMaterial: MaterialInstance? = null
) : Node(engine) {

    private val tag = "SignBoardNode"

    private var signTexture: Texture? = null
    private var signMaterialInstance: MaterialInstance? = null
    private var signPlaneNode: PlaneNode? = null

    init {
        // =========================================================================
        // 1. VERTICAL MINE TIMBER SUPPORT BEAM (Upright Mine Prop / Post)
        // Authentic rough-hewn timber post extending from floor (Y = 0m) to tunnel wall/ceiling (Y = 2.4m)
        // =========================================================================
        val woodMat = timberMaterial ?: metalMaterial
        val post = CubeNode(
            engine = engine,
            size = Float3(0.22f, 2.40f, 0.22f),
            materialInstance = woodMat
        ).apply {
            position = Float3(0.0f, 1.20f, -0.12f)
        }
        addChildNode(post)

        // Horizontal timber mounting bracket behind the sign
        val crossBracket = CubeNode(
            engine = engine,
            size = Float3(0.86f, 0.16f, 0.06f),
            materialInstance = woodMat
        ).apply {
            position = Float3(0.0f, 1.40f, -0.04f)
        }
        addChildNode(crossBracket)

        // =========================================================================
        // 2. INDUSTRIAL DARK STEEL BACKING PLATE
        // Weathered enameled steel backing frame with protective border
        // =========================================================================
        val darkMetal = metalMaterial ?: woodMat
        val backingPlate = CubeNode(
            engine = engine,
            size = Float3(0.80f, 0.48f, 0.02f),
            materialInstance = darkMetal
        ).apply {
            position = Float3(0.0f, 1.40f, -0.005f)
        }
        addChildNode(backingPlate)

        // =========================================================================
        // 3. AUTHENTIC SIGN-BOARD FACE (textures/sign_board.png)
        // Aspect ratio 1437 x 855 -> 0.78m x 0.46m (1.69:1) at eye level (Y = 1.40m)
        // =========================================================================
        try {
            val rawBmp = context.assets.open("textures/sign_board.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }

            if (rawBmp != null) {
                val tex = Texture.Builder()
                    .width(rawBmp.width)
                    .height(rawBmp.height)
                    .sampler(Texture.Sampler.SAMPLER_2D)
                    .format(Texture.InternalFormat.RGBA8)
                    .build(engine)

                TextureHelper.setBitmap(engine, tex, 0, rawBmp)
                tex.generateMipmaps(engine)
                rawBmp.recycle()
                signTexture = tex

                val mat = materialLoader.createTextureInstance(
                    texture = tex,
                    isOpaque = true,
                    metallic = 0.05f,
                    roughness = 0.85f,
                    reflectance = 0.35f
                )
                signMaterialInstance = mat

                val plane = PlaneNode(
                    engine = engine,
                    size = Float3(0.78f, 0.46f, 0.0f),
                    center = Float3(0.0f, 1.40f, 0.012f),
                    normal = Float3(0.0f, 0.0f, 1.0f),
                    uvScale = Float2(1.0f, 1.0f),
                    materialInstance = mat
                ).apply {
                    setCulling(false)
                }
                addChildNode(plane)
                signPlaneNode = plane
                Log.i(tag, "SignBoardNode: Successfully loaded and textured turn right sign")
            }
        } catch (e: Exception) {
            Log.e(tag, "SignBoardNode: Error loading sign texture: ${e.message}", e)
        }

        // =========================================================================
        // 4. CORNER INDUSTRIAL MOUNTING BOLTS
        // 4 rusted hex bolts matching the 4 corner bolts on the authentic sign plate
        // =========================================================================
        val boltRadius = 0.012f
        val boltHeight = 0.010f
        val boltPositions = listOf(
            Float3(-0.36f, 1.40f + 0.20f, 0.018f),
            Float3(0.36f, 1.40f + 0.20f, 0.018f),
            Float3(-0.36f, 1.40f - 0.20f, 0.018f),
            Float3(0.36f, 1.40f - 0.20f, 0.018f)
        )
        for (bPos in boltPositions) {
            try {
                val bolt = CylinderNode(
                    engine = engine,
                    radius = boltRadius,
                    height = boltHeight,
                    materialInstance = darkMetal
                ).apply {
                    position = bPos
                    rotation = Float3(90.0f, 0.0f, 0.0f) // Faces outward
                }
                addChildNode(bolt)
            } catch (e: Exception) {
                Log.w(tag, "Failed to create bolt at $bPos: ${e.message}")
            }
        }

        // =========================================================================
        // 5. DEDICATED SIGN ILLUMINATION (Warm Incandescent Mine Lamp)
        // Illuminates the sign so the TURN RIGHT text and arrow are vividly readable
        // =========================================================================
        try {
            val lampBuilder = LightManager.Builder(LightManager.Type.POINT)
                .color(1.0f, 0.90f, 0.72f) // Warm miner incandescent glow
                .intensity(30000f)
                .falloff(4.0f)
            val lampNode = LightNode(engine, builder = lampBuilder).apply {
                position = Float3(0.0f, 1.72f, 0.35f)
            }
            addChildNode(lampNode)

            // Small hooded lamp fixture housing
            val fixture = CylinderNode(
                engine = engine,
                radius = 0.035f,
                height = 0.06f,
                materialInstance = darkMetal
            ).apply {
                position = Float3(0.0f, 1.75f, 0.25f)
                rotation = Float3(45.0f, 0.0f, 0.0f)
            }
            addChildNode(fixture)
        } catch (e: Exception) {
            Log.w(tag, "Failed to create sign illumination lamp: ${e.message}")
        }
    }

    override fun destroy() {
        signPlaneNode?.isVisible = false
        val mat = signMaterialInstance
        if (mat != null) {
            try {
                engine.destroyMaterialInstance(mat)
            } catch (_: Exception) {}
            signMaterialInstance = null
        }
        val tex = signTexture
        if (tex != null) {
            try {
                engine.destroyTexture(tex)
            } catch (_: Exception) {}
            signTexture = null
        }
        super.destroy()
    }
}
