package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.sin

/**
 * Wall-Mounted Interactive Fire Alarm / Pull-Down Station (Module 01: Electrical Fire Safety):
 * - Loads authentic 3D model: models/simple_fire_alarm.glb (US Pull-Down Station)
 * - Scaled to realistic industrial dimensions (~0.40m height, ~0.28m width, ~0.11m thickness)
 * - Wall-mounted flush against right mine rock wall at chest height (Y = 1.25m)
 * - Interactive Pull-Down Lever: visually pulls down when activated by pinch gesture or tap
 * - Emergency Strobe/Indicator LED: dynamic pulsing red beacon active during evacuation
 * - Triggers mine-wide evacuation siren and advances training sequence:
 *   FIRE ALARM -> FIRE EXTINGUISHER -> ELECTRICAL FIRE
 */
class FireAlarmNode(
    engine: Engine,
    modelLoader: ModelLoader,
    indicatorMaterial: MaterialInstance? = null
) : Node(engine) {

    private val tag = "FireAlarmNode"
    var modelNode: ModelNode? = null
        private set

    // Interactive target for gestures / touch
    val alarmInteractiveTarget: Node = Node(engine)

    // Emergency flashing strobe LED on the alarm face
    val indicatorStrobeNode: Node = Node(engine)
    private var indicatorLight: CylinderNode? = null

    var isPulled: Boolean = false
        private set

    private var strobeTimerNanos: Long = -1L

    init {
        try {
            val modelInstance = modelLoader.createModelInstance("models/simple_fire_alarm.glb")
            // Centering offset and realistic scale:
            // Height = ~0.40m, Width = ~0.28m, Thickness = ~0.11m
            val scaleFactor = 0.08f
            val mNode = ModelNode(
                modelInstance = modelInstance,
                autoAnimate = false
            ).apply {
                position = Float3(0.0f, -1.0558f * scaleFactor, 0.0f)
                scale = Float3(scaleFactor, scaleFactor, scaleFactor)
            }
            addChildNode(mNode)
            modelNode = mNode
            Log.i(tag, "simple_fire_alarm.glb successfully loaded and mounted")
        } catch (e: Exception) {
            Log.e(tag, "Error loading simple_fire_alarm.glb: ${e.message}", e)
        }

        // Interactive touch / proximity target centered on the alarm face
        alarmInteractiveTarget.position = Float3(0.0f, 0.0f, 0.0f)
        addChildNode(alarmInteractiveTarget)

        // Emergency flashing indicator LED on alarm face (lower right)
        if (indicatorMaterial != null) {
            val led = CylinderNode(
                engine,
                radius = 0.018f,
                height = 0.015f,
                materialInstance = indicatorMaterial
            ).apply {
                position = Float3(-0.065f, -0.105f, 0.092f)
                rotation = Float3(0f, 0f, 90f)
                isVisible = false
            }
            indicatorLight = led
            indicatorStrobeNode.addChildNode(led)
            addChildNode(indicatorStrobeNode)
        }
    }

    fun triggerAlarm() {
        if (isPulled) return
        isPulled = true
        try {
            modelNode?.playAnimation(0)
        } catch (e: Exception) {
            Log.w(tag, "Could not play PullDown animation: ${e.message}")
        }
        indicatorLight?.isVisible = true
        Log.i(tag, "Fire Alarm activated: lever pulled down, emergency siren initiated")
    }

    override fun onFrame(frameTimeNanos: Long) {
        super.onFrame(frameTimeNanos)
        if (isPulled && indicatorLight != null) {
            if (strobeTimerNanos < 0) strobeTimerNanos = frameTimeNanos
            val t = (frameTimeNanos - strobeTimerNanos).toDouble() / 1_000_000_000.0
            // Rapid emergency pulsing (5 Hz)
            val flash = (sin(t * 31.4) > 0.0)
            indicatorLight?.isVisible = flash
            val scalePulse = if (flash) 1.25f else 0.85f
            indicatorLight?.scale = Float3(scalePulse, scalePulse, scalePulse)
        }
    }

    override fun destroy() {
        indicatorLight = null
        super.destroy()
    }
}
