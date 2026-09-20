package com.minesafe.ar.ar

import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.Node
import io.github.sceneview.node.PlaneNode

/**
 * Video Fire Hazard Node:
 * - Plays the uploaded MP4 fire video: FootageCrate-Brief_Aerial_Fire_Burst.mp4
 * - Positioned directly in front of the wall-mounted electrical box
 * - Uses Sceneview video chroma-key material with Color.BLACK so the black background is 100% transparent,
 *   leaving only the vibrant, luminous fire flames, embers, and smoke billows visible in AR.
 * - Continuous infinite looping while hazard is active.
 * - Programmatic controls: startFire(), stopFire(), isFireRunning()
 * - Strictly module-scoped to Module 01 (Electrical Fire Safety); never instantiated in Module 02.
 */
class VideoFireNode(
    engine: Engine,
    materialLoader: MaterialLoader,
    private val context: Context,
    val planeSize: Float3 = Float3(1.10f, 0.65f, 0.0f)
) : Node(engine) {

    private val tag = "VideoFireNode"
    var planeNode: PlaneNode? = null
        private set
    var mediaPlayer: MediaPlayer? = null
        private set

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var filamentStream: Stream? = null
    private var filamentTexture: Texture? = null
    private var videoMaterialInstance: MaterialInstance? = null

    private var isRunning: Boolean = false

    init {
        try {
            // 1. Setup SurfaceTexture and Filament Stream
            val sTexture = SurfaceTexture(0).apply {
                detachFromGLContext()
            }
            surfaceTexture = sTexture
            val surf = Surface(sTexture)
            surface = surf

            val stream = Stream.Builder()
                .stream(sTexture)
                .build(engine)
            filamentStream = stream

            val texture = Texture.Builder()
                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                .format(Texture.InternalFormat.RGB8)
                .build(engine)
            texture.setExternalStream(engine, stream)
            filamentTexture = texture

            // 2. Create Chroma-Key Video Material Instance (Color.BLACK transparent)
            val matInstance = materialLoader.createVideoInstance(texture, Color.BLACK)
            videoMaterialInstance = matInstance

            // 3. Setup PlaneNode positioned to engulf the electrical box
            val pNode = PlaneNode(
                engine = engine,
                size = planeSize,
                center = Float3(0.0f, 0.0f, 0.0f),
                normal = Float3(0.0f, 0.0f, 1.0f),
                uvScale = Float2(1.0f, 1.0f),
                materialInstance = matInstance
            )
            addChildNode(pNode)
            planeNode = pNode

            // 4. Initialize MediaPlayer with the MP4 video and bind to the Surface
            val player = MediaPlayer().apply {
                val afd = try {
                    context.resources.openRawResourceFd(com.minesafe.ar.R.raw.fire_video)
                } catch (e: Exception) {
                    try {
                        context.assets.openFd("videos/fire_video.mp4")
                    } catch (e2: Exception) {
                        context.assets.openFd("FootageCrate-Brief_Aerial_Fire_Burst.mp4")
                    }
                }
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                setSurface(surf)
                isLooping = true
                setVolume(0.0f, 0.0f) // Audio is handled by AudioManager sound effects
                prepare()
                start()
            }
            mediaPlayer = player
            isRunning = true
            Log.i(tag, "VideoFireNode initialized and looping fire video started successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error initializing VideoFireNode: ${e.message}", e)
        }
    }

    fun isFireRunning(): Boolean = isRunning

    fun startFire() {
        try {
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                }
            }
            isRunning = true
            planeNode?.isVisible = true
            isVisible = true
            Log.i(tag, "startFire: video playback started/resumed")
        } catch (e: Exception) {
            Log.w(tag, "Error in startFire: ${e.message}")
        }
    }

    fun stopFire() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                }
            }
            isRunning = false
            planeNode?.isVisible = false
            isVisible = false
            Log.i(tag, "stopFire: video playback stopped and hidden")
        } catch (e: Exception) {
            Log.w(tag, "Error in stopFire: ${e.message}")
        }
    }

    // Legacy aliases for backward compatibility with existing training calls
    fun startFireAnimation() = startFire()
    fun stopFireAnimation() = stopFire()
    fun isFireAnimationRunning(): Boolean = isFireRunning()
    fun stopAnimation() = stopFire()

    override fun destroy() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MediaPlayer: ${e.message}")
        }
        try {
            surface?.release()
            surface = null
            surfaceTexture?.release()
            surfaceTexture = null
        } catch (e: Exception) {
            Log.w(tag, "Error releasing surface: ${e.message}")
        }
        try {
            filamentStream?.let { engine.destroyStream(it) }
            filamentStream = null
            filamentTexture?.let { engine.destroyTexture(it) }
            filamentTexture = null
        } catch (e: Exception) {
            Log.w(tag, "Error destroying Filament video resources: ${e.message}")
        }
        super.destroy()
    }
}
