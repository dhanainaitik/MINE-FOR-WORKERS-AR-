package com.minesafe.ar.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.Node
import io.github.sceneview.node.PlaneNode
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Volumetric Extinguishing Fog Spray Node:
 * - Replaces flat 2D video planes with a dense, camera-facing volumetric particle system.
 * - Sourced directly from keyframes of the provided fog video asset (feathered zero-alpha puff textures).
 * - Multi-particle stream (32 particles) issuing continuously from the fire extinguisher nozzle.
 * - Each particle is camera-oriented on every frame, eliminating flat edges, rectangular cards, or edge-on lines from ANY angle.
 * - High-velocity core jet + expanding turbulent conical billow that disperses and fades naturally at the fire base.
 * - Follows the nozzle's orientation and movement in real time as the player aims and sweeps.
 * - Continuous infinite looping while discharging; immediately hides and stops when released or extinguished.
 */
class VideoSprayNode(
    engine: Engine,
    materialLoader: MaterialLoader,
    private val context: Context,
    var cameraNode: Node? = null
) : Node(engine) {

    private val tag = "VideoSprayNode"

    private val particleCount = 32
    private val particles = ArrayList<SprayParticle>(particleCount)

    private val textures = ArrayList<Texture>()
    private val materials = ArrayList<MaterialInstance>()

    private var isSpraying: Boolean = false
    private var lastUpdateTimeMs: Long = 0L
    private var cachedCameraRot: Float3 = Float3(0f, 0f, 0f)

    private class SprayParticle(
        val node: PlaneNode,
        var progress: Float,
        val speed: Float,
        val baseAngle: Float,
        val swirlSpeed: Float,
        val radialFactor: Float,
        val baseScale: Float,
        var roll: Float,
        val rollSpeed: Float,
        val isCoreJet: Boolean
    )

    init {
        try {
            // 1. Load authentic smoke puff textures sourced directly from fog video frames
            val assetPaths = listOf(
                "textures/smoke_puff_1.png",
                "textures/smoke_puff_2.png",
                "textures/smoke_puff_3.png",
                "textures/smoke_puff_4.png"
            )

            for (path in assetPaths) {
                try {
                    val stream = context.assets.open(path)
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream.close()

                    val tex = Texture.Builder()
                        .width(bmp.width)
                        .height(bmp.height)
                        .sampler(Texture.Sampler.SAMPLER_2D)
                        .format(Texture.InternalFormat.RGBA8)
                        .build(engine)

                    TextureHelper.setBitmap(engine, tex, 0, bmp)
                    bmp.recycle()

                    val mat = materialLoader.createTextureInstance(tex, isOpaque = false)
                    textures.add(tex)
                    materials.add(mat)
                } catch (e: Exception) {
                    Log.w(tag, "Could not load $path: ${e.message}")
                }
            }

            // Fallback texture if assets were somehow missing
            if (materials.isEmpty()) {
                val fallbackBmp = createFallbackPuffBitmap()
                val tex = Texture.Builder()
                    .width(fallbackBmp.width)
                    .height(fallbackBmp.height)
                    .sampler(Texture.Sampler.SAMPLER_2D)
                    .format(Texture.InternalFormat.RGBA8)
                    .build(engine)
                TextureHelper.setBitmap(engine, tex, 0, fallbackBmp)
                fallbackBmp.recycle()
                val mat = materialLoader.createTextureInstance(tex, isOpaque = false)
                textures.add(tex)
                materials.add(mat)
            }

            // 2. Initialize 32 volumetric spray particles
            for (i in 0 until particleCount) {
                val mat = materials[i % materials.size]
                val plane = PlaneNode(
                    engine = engine,
                    size = Float3(1.0f, 1.0f, 0.0f),
                    center = Float3(0.0f, 0.0f, 0.0f),
                    normal = Float3(0.0f, 0.0f, 1.0f),
                    uvScale = Float2(1.0f, 1.0f),
                    materialInstance = mat
                ).apply {
                    isVisible = false
                }
                addChildNode(plane)

                // First 6 particles form the tight, high-speed core nozzle jet
                val isCore = i < 6
                val speed = if (isCore) 2.2f + (i * 0.15f) else 1.25f + ((i % 5) * 0.10f)
                val baseAngle = (i * 2.39996f) // Golden angle distribution
                val swirlSpeed = if (i % 2 == 0) 1.8f else -1.8f
                val radialFactor = if (isCore) 0.20f + (i * 0.08f) else 0.40f + ((i % 7) * 0.10f)
                val baseScale = if (isCore) 0.65f else 0.85f + ((i % 4) * 0.12f)
                val initialRoll = (i * 47f) % 360f
                val rollSpeed = if (i % 2 == 0) 35f else -35f

                // Evenly stagger progress so the spray is continuous with zero gaps
                val initialProgress = (i.toFloat() / particleCount.toFloat())

                particles.add(
                    SprayParticle(
                        node = plane,
                        progress = initialProgress,
                        speed = speed,
                        baseAngle = baseAngle,
                        swirlSpeed = swirlSpeed,
                        radialFactor = radialFactor,
                        baseScale = baseScale,
                        roll = initialRoll,
                        rollSpeed = rollSpeed,
                        isCoreJet = isCore
                    )
                )
            }

            isVisible = false
            Log.i(tag, "VideoSprayNode initialized with $particleCount 3D volumetric particles from fog video textures")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing VideoSprayNode: ${e.message}", e)
        }
    }

    private fun createFallbackPuffBitmap(): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val center = size / 2f
        val maxR = size / 2f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = (x - center) / maxR
                val dy = (y - center) / maxR
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < 0.90f) {
                    val alpha = (0.5f - 0.5f * cos((0.90f - dist) / 0.90f * Math.PI.toFloat())).coerceIn(0f, 1f)
                    bmp.setPixel(x, y, Color.argb((alpha * 240f).toInt(), 255, 255, 255))
                } else {
                    bmp.setPixel(x, y, 0)
                }
            }
        }
        return bmp
    }

    fun isSpraying(): Boolean = isSpraying

    fun startSpray() {
        if (isSpraying) return
        isSpraying = true
        isVisible = true
        lastUpdateTimeMs = SystemClock.uptimeMillis()

        for (p in particles) {
            p.node.isVisible = true
        }
        Log.i(tag, "startSpray: 3D Volumetric fog spray active")
    }

    fun stopSpray() {
        if (!isSpraying) return
        isSpraying = false
        isVisible = false

        for (p in particles) {
            p.node.isVisible = false
        }
        Log.i(tag, "stopSpray: 3D Volumetric fog spray stopped and hidden")
    }

    fun updateCameraOrientation(camRot: Float3) {
        cachedCameraRot = camRot
        if (isSpraying) {
            val now = SystemClock.uptimeMillis()
            val dt = if (lastUpdateTimeMs == 0L) 0.016f else ((now - lastUpdateTimeMs) / 1000f).coerceIn(0.001f, 0.05f)
            lastUpdateTimeMs = now
            updateSimulation(dt, camRot)
        }
    }

    override fun onFrame(frameTimeNanos: Long) {
        super.onFrame(frameTimeNanos)
        if (!isSpraying) return

        val now = SystemClock.uptimeMillis()
        val dt = if (lastUpdateTimeMs == 0L) 0.016f else ((now - lastUpdateTimeMs) / 1000f).coerceIn(0.001f, 0.05f)
        lastUpdateTimeMs = now

        val camRot = cameraNode?.worldRotation ?: cachedCameraRot
        updateSimulation(dt, camRot)
    }

    private fun updateSimulation(dt: Float, camRot: Float3) {
        val totalDistance = 2.40f // Reaches and engulfs the electrical fire at ~2m

        for (p in particles) {
            // Advance particle along spray trajectory
            p.progress += p.speed * dt
            if (p.progress >= 1.0f) {
                p.progress -= 1.0f
            }

            val prog = p.progress
            p.roll = (p.roll + p.rollSpeed * dt) % 360f

            // Forward travel along -Z (local nozzle direction)
            val z = if (p.isCoreJet) {
                -0.95f * prog // High-pressure tight core near nozzle
            } else {
                -totalDistance * prog // Expanding cloud travelling to fire
            }

            // Conical radial dispersion
            val coneRadius = if (p.isCoreJet) {
                0.025f + 0.12f * prog
            } else {
                0.04f + 0.52f * Math.pow(prog.toDouble(), 1.15).toFloat()
            }

            val swirl = p.baseAngle + (p.swirlSpeed * prog * 2.5f)
            val r = coneRadius * p.radialFactor
            val x = r * cos(swirl)
            // Downward slope towards fire base (-0.16m drop at 2.4m)
            val y = (-0.16f * prog) + (r * sin(swirl))

            p.node.position = Float3(x, y, z)

            // Dynamic volumetric scale expansion:
            // Starts small at nozzle orifice (~0.12m) and billows to ~0.95m at fire
            val scaleVal = if (p.isCoreJet) {
                p.baseScale * (0.12f + 0.35f * prog)
            } else {
                val growth = 0.14f + 0.82f * Math.pow(prog.toDouble(), 0.65).toFloat()
                // Natural dissipation / soft tapering at the very end of spray
                val fadeFactor = if (prog > 0.85f) 1.0f - ((prog - 0.85f) / 0.15f) * 0.30f else 1.0f
                p.baseScale * growth * fadeFactor
            }

            p.node.scale = Float3(scaleVal, scaleVal, 1.0f)

            // CRITICAL: Billboarding toward camera orientation with random in-plane roll.
            // Eliminates flat cards or edge-on lines from ANY camera angle!
            p.node.worldRotation = Float3(camRot.x, camRot.y, camRot.z + p.roll)
        }
    }

    override fun destroy() {
        stopSpray()
        for (tex in textures) {
            try {
                engine.destroyTexture(tex)
            } catch (e: Exception) {
                Log.w(tag, "Error destroying texture: ${e.message}")
            }
        }
        textures.clear()
        materials.clear()
        particles.clear()
        super.destroy()
    }
}
