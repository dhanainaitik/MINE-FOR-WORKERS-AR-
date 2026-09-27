package com.minesafe.ar.ar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 3D Holographic Evacuation Safety Guide System for MineSafe AR:
 *
 * Full-body articulated procedural humanoid character that guides mine workers to safety:
 * - Emerges automatically upon fire alarm activation.
 * - Materialized with futuristic cyan/blue translucent glow, AR scanline rings, and headlamp illumination.
 * - Dynamic procedural running kinematics (articulated hips, knees, calves, boots, shoulders, elbows, and torso).
 * - Stride-synchronized animation matching forward ground speed (zero sliding or moonwalking).
 * - Navigates precisely along the mine centerline using [MineNavigationSystem] waypoints:
 *     1. Alarm Station / Starting Corridor (Z = -4.2m)
 *     2. Main Mine Corridor (past Extinguisher & Fire Hazards)
 *     3. Junction with "TURN RIGHT" Sign: Smoothly veers RIGHT, entering the second mine corridor (avoiding wrong left curve)
 *     4. Traverses full Second Mine Corridor along railway tracks
 *     5. Follows the final LEFT CURVE to the terminus evacuation exit
 *     6. Triggers completion state with celebratory beacon flare and guidance signal.
 */
class EvacuationHologramNode(
    engine: Engine,
    bodyMaterial: MaterialInstance?,
    accentMaterial: MaterialInstance?,
    headlampMaterial: MaterialInstance?,
    private val navSystem: MineNavigationSystem
) : Node(engine) {

    private val tag = "EvacuationHologram"

    // Hologram State
    var isEvacuationActive: Boolean = false
        private set
    var isCompleted: Boolean = false
        private set
    var currentProgress: Float = 0f
        private set

    // Optional callback when evacuation terminus is successfully reached
    var onEvacuationComplete: (() -> Unit)? = null

    // Base kinematic joints
    private val rootRig: Node = Node(engine)
    private val pelvisJoint: Node = Node(engine)
    private val torsoJoint: Node = Node(engine)

    // Limb joints for running animation
    private val leftHipJoint: Node = Node(engine)
    private val rightHipJoint: Node = Node(engine)
    private val leftKneeJoint: Node = Node(engine)
    private val rightKneeJoint: Node = Node(engine)

    private val leftShoulderJoint: Node = Node(engine)
    private val rightShoulderJoint: Node = Node(engine)
    private val leftElbowJoint: Node = Node(engine)
    private val rightElbowJoint: Node = Node(engine)

    // Hologram visual FX
    private val scanlineRing: CylinderNode
    private val floatingChevronGroup: Node = Node(engine)
    private var hologramLight: LightNode? = null

    // Navigation and timing state
    private var currentYaw: Float = 0f
    private var runPhase: Float = 0f
    private var totalElapsedSec: Float = 0f
    private var spawnTimer: Float = 0f
    private var completionTimer: Float = 0f
    private var lastFrameTimeNanos: Long = -1L

    companion object {
        private const val SPAWN_DURATION = 0.9f
        private const val COMPLETION_DISPLAY_DURATION = 5.0f
        private const val STRIDE_LENGTH = 1.35f
        private const val NORMAL_RUN_SPEED = 2.45f
        private const val JUNCTION_APPROACH_SPEED = 1.85f
        private const val TERMINUS_APPROACH_SPEED = 1.35f
        private const val BASE_LIGHT_INTENSITY = 22000f
    }

    init {
        addChildNode(rootRig)
        rootRig.addChildNode(pelvisJoint)

        // =========================================================================
        // 1. PELVIS & TORSO HIERARCHY
        // =========================================================================
        // Ground to pelvis height = 0.86m
        pelvisJoint.position = Float3(0f, 0.86f, 0f)

        // Pelvis mesh
        val pelvisMesh = CubeNode(
            engine,
            size = Float3(0.28f, 0.12f, 0.18f),
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, 0f, 0f)
        }
        pelvisJoint.addChildNode(pelvisMesh)

        // Torso joint sits directly above pelvis
        torsoJoint.position = Float3(0f, 0.08f, 0f)
        pelvisJoint.addChildNode(torsoJoint)

        // Lower abdomen
        val abdomenMesh = CubeNode(
            engine,
            size = Float3(0.26f, 0.16f, 0.16f),
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, 0.09f, 0f)
        }
        torsoJoint.addChildNode(abdomenMesh)

        // Upper chest
        val chestMesh = CubeNode(
            engine,
            size = Float3(0.32f, 0.24f, 0.19f),
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, 0.28f, 0f)
        }
        torsoJoint.addChildNode(chestMesh)

        // Reflective Safety Harness Crossed Straps (Cyan Luminous Bands)
        val harnessStrap1 = CubeNode(
            engine,
            size = Float3(0.035f, 0.30f, 0.015f),
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.28f, 0.10f)
            rotation = Float3(0f, 0f, 28f)
        }
        val harnessStrap2 = CubeNode(
            engine,
            size = Float3(0.035f, 0.30f, 0.015f),
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.28f, 0.10f)
            rotation = Float3(0f, 0f, -28f)
        }
        torsoJoint.addChildNode(harnessStrap1)
        torsoJoint.addChildNode(harnessStrap2)

        // Waist Safety Utility Belt
        val utilityBelt = CubeNode(
            engine,
            size = Float3(0.30f, 0.045f, 0.19f),
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.02f, 0f)
        }
        torsoJoint.addChildNode(utilityBelt)

        // =========================================================================
        // 2. HEAD & MINER SAFETY HELMET
        // =========================================================================
        val headJoint = Node(engine).apply {
            position = Float3(0f, 0.44f, 0f)
        }
        torsoJoint.addChildNode(headJoint)

        // Neck
        val neckMesh = CylinderNode(
            engine,
            radius = 0.048f,
            height = 0.08f,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -0.01f, 0f)
        }
        headJoint.addChildNode(neckMesh)

        // Head
        val headMesh = SphereNode(
            engine,
            radius = 0.11f,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, 0.11f, 0f)
            scale = Float3(0.9f, 1.15f, 1.0f)
        }
        headJoint.addChildNode(headMesh)

        // Miner Hard-Hat Dome
        val helmetDome = SphereNode(
            engine,
            radius = 0.125f,
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.17f, 0f)
            scale = Float3(1.05f, 0.85f, 1.15f)
        }
        headJoint.addChildNode(helmetDome)

        // Miner Hard-Hat Brim
        val helmetBrim = CylinderNode(
            engine,
            radius = 0.155f,
            height = 0.018f,
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.14f, -0.02f)
        }
        headJoint.addChildNode(helmetBrim)

        // Miner Headlamp Fixture (projects forward toward -Z)
        val headlampFixture = CylinderNode(
            engine,
            radius = 0.032f,
            height = 0.035f,
            materialInstance = headlampMaterial
        ).apply {
            position = Float3(0f, 0.18f, -0.135f)
            rotation = Float3(90f, 0f, 0f)
        }
        headJoint.addChildNode(headlampFixture)

        // Miner Headlamp Lens / Spot
        val headlampSpot = SphereNode(
            engine,
            radius = 0.028f,
            materialInstance = headlampMaterial
        ).apply {
            position = Float3(0f, 0.18f, -0.155f)
        }
        headJoint.addChildNode(headlampSpot)

        // =========================================================================
        // 3. LEGS & BOOTS (Left and Right Kinematic Chains)
        // =========================================================================
        val legSpacingX = 0.105f
        val thighLength = 0.38f
        val calfLength = 0.36f

        // LEFT LEG
        leftHipJoint.position = Float3(-legSpacingX, 0f, 0f)
        pelvisJoint.addChildNode(leftHipJoint)

        val leftThighMesh = CylinderNode(
            engine,
            radius = 0.055f,
            height = thighLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -thighLength / 2f, 0f)
        }
        leftHipJoint.addChildNode(leftThighMesh)

        leftKneeJoint.position = Float3(0f, -thighLength, 0f)
        leftHipJoint.addChildNode(leftKneeJoint)

        val leftKneeCap = SphereNode(
            engine,
            radius = 0.052f,
            materialInstance = accentMaterial
        )
        leftKneeJoint.addChildNode(leftKneeCap)

        val leftCalfMesh = CylinderNode(
            engine,
            radius = 0.046f,
            height = calfLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -calfLength / 2f, 0f)
        }
        leftKneeJoint.addChildNode(leftCalfMesh)

        // Miner Heavy Boot (extends forward toward -Z)
        val leftBoot = CubeNode(
            engine,
            size = Float3(0.10f, 0.085f, 0.22f),
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, -calfLength, -0.045f)
        }
        leftKneeJoint.addChildNode(leftBoot)

        // RIGHT LEG
        rightHipJoint.position = Float3(legSpacingX, 0f, 0f)
        pelvisJoint.addChildNode(rightHipJoint)

        val rightThighMesh = CylinderNode(
            engine,
            radius = 0.055f,
            height = thighLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -thighLength / 2f, 0f)
        }
        rightHipJoint.addChildNode(rightThighMesh)

        rightKneeJoint.position = Float3(0f, -thighLength, 0f)
        rightHipJoint.addChildNode(rightKneeJoint)

        val rightKneeCap = SphereNode(
            engine,
            radius = 0.052f,
            materialInstance = accentMaterial
        )
        rightKneeJoint.addChildNode(rightKneeCap)

        val rightCalfMesh = CylinderNode(
            engine,
            radius = 0.046f,
            height = calfLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -calfLength / 2f, 0f)
        }
        rightKneeJoint.addChildNode(rightCalfMesh)

        val rightBoot = CubeNode(
            engine,
            size = Float3(0.10f, 0.085f, 0.22f),
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, -calfLength, -0.045f)
        }
        rightKneeJoint.addChildNode(rightBoot)

        // =========================================================================
        // 4. ARMS & HANDS (Left and Right Kinematic Chains)
        // =========================================================================
        val shoulderSpacingX = 0.21f
        val shoulderY = 0.35f
        val upperArmLength = 0.28f
        val forearmLength = 0.25f

        // LEFT ARM
        leftShoulderJoint.position = Float3(-shoulderSpacingX, shoulderY, 0f)
        torsoJoint.addChildNode(leftShoulderJoint)

        val leftShoulderCap = SphereNode(
            engine,
            radius = 0.055f,
            materialInstance = accentMaterial
        )
        leftShoulderJoint.addChildNode(leftShoulderCap)

        val leftUpperArmMesh = CylinderNode(
            engine,
            radius = 0.042f,
            height = upperArmLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -upperArmLength / 2f, 0f)
        }
        leftShoulderJoint.addChildNode(leftUpperArmMesh)

        leftElbowJoint.position = Float3(0f, -upperArmLength, 0f)
        leftShoulderJoint.addChildNode(leftElbowJoint)

        val leftElbowCap = SphereNode(
            engine,
            radius = 0.042f,
            materialInstance = accentMaterial
        )
        leftElbowJoint.addChildNode(leftElbowCap)

        val leftForearmMesh = CylinderNode(
            engine,
            radius = 0.038f,
            height = forearmLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -forearmLength / 2f, 0f)
        }
        leftElbowJoint.addChildNode(leftForearmMesh)

        val leftHand = SphereNode(
            engine,
            radius = 0.042f,
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, -forearmLength, 0f)
        }
        leftElbowJoint.addChildNode(leftHand)

        // RIGHT ARM
        rightShoulderJoint.position = Float3(shoulderSpacingX, shoulderY, 0f)
        torsoJoint.addChildNode(rightShoulderJoint)

        val rightShoulderCap = SphereNode(
            engine,
            radius = 0.055f,
            materialInstance = accentMaterial
        )
        rightShoulderJoint.addChildNode(rightShoulderCap)

        val rightUpperArmMesh = CylinderNode(
            engine,
            radius = 0.042f,
            height = upperArmLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -upperArmLength / 2f, 0f)
        }
        rightShoulderJoint.addChildNode(rightUpperArmMesh)

        rightElbowJoint.position = Float3(0f, -upperArmLength, 0f)
        rightShoulderJoint.addChildNode(rightElbowJoint)

        val rightElbowCap = SphereNode(
            engine,
            radius = 0.042f,
            materialInstance = accentMaterial
        )
        rightElbowJoint.addChildNode(rightElbowCap)

        val rightForearmMesh = CylinderNode(
            engine,
            radius = 0.038f,
            height = forearmLength,
            materialInstance = bodyMaterial
        ).apply {
            position = Float3(0f, -forearmLength / 2f, 0f)
        }
        rightElbowJoint.addChildNode(rightForearmMesh)

        val rightHand = SphereNode(
            engine,
            radius = 0.042f,
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, -forearmLength, 0f)
        }
        rightElbowJoint.addChildNode(rightHand)

        // =========================================================================
        // 5. HOLOGRAPHIC SCANLINE RING & FLOATING CHEVRON
        // =========================================================================
        // Vertical sweeping scanline disc
        scanlineRing = CylinderNode(
            engine,
            radius = 0.28f,
            height = 0.012f,
            materialInstance = accentMaterial
        ).apply {
            position = Float3(0f, 0.85f, 0f)
        }
        rootRig.addChildNode(scanlineRing)

        // Floating Downward Evacuation Guidance Chevron above helmet
        floatingChevronGroup.position = Float3(0f, 1.95f, 0f)
        val chevronWingL = CubeNode(
            engine,
            size = Float3(0.12f, 0.024f, 0.02f),
            materialInstance = headlampMaterial
        ).apply {
            position = Float3(-0.045f, 0f, 0f)
            rotation = Float3(0f, 0f, 32f)
        }
        val chevronWingR = CubeNode(
            engine,
            size = Float3(0.12f, 0.024f, 0.02f),
            materialInstance = headlampMaterial
        ).apply {
            position = Float3(0.045f, 0f, 0f)
            rotation = Float3(0f, 0f, -32f)
        }
        floatingChevronGroup.addChildNode(chevronWingL)
        floatingChevronGroup.addChildNode(chevronWingR)
        rootRig.addChildNode(floatingChevronGroup)

        // =========================================================================
        // 6. ATMOSPHERIC CYAN HOLOGRAPHIC LIGHT BEACON
        // =========================================================================
        try {
            val lightBuilder = LightManager.Builder(LightManager.Type.POINT)
                .color(0.0f, 0.88f, 1.0f) // Vibrant futuristic cyan glow
                .intensity(BASE_LIGHT_INTENSITY)
                .falloff(4.5f)
            val light = LightNode(engine, builder = lightBuilder).apply {
                position = Float3(0f, 1.35f, -0.15f)
            }
            hologramLight = light
            rootRig.addChildNode(light)
        } catch (e: Exception) {
            Log.w(tag, "Failed to initialize hologram light: ${e.message}")
        }

        // Initially hidden until emergency alarm trigger
        isVisible = false
        rootRig.scale = Float3(0.01f, 0.01f, 0.01f)
    }

    /**
     * Deploys the evacuation hologram at the emergency starting point (near the fire alarm).
     */
    fun startEvacuation(startProgress: Float = 4.20f) {
        if (isEvacuationActive) return
        isEvacuationActive = true
        isCompleted = false
        currentProgress = startProgress
        spawnTimer = 0f
        completionTimer = 0f
        runPhase = 0f
        isVisible = true

        // Place near the wall fire alarm on right side of tunnel: X = 1.25m, Z = -4.20m
        position = Float3(1.25f, 0.00f, -4.20f)
        currentYaw = -20f
        rotation = Float3(0f, currentYaw, 0f)
        rootRig.scale = Float3(0.05f, 0.05f, 0.05f)

        Log.i(tag, "Evacuation Hologram deployed at Z = -4.20m. Commencing guide sequence.")
    }

    /**
     * Continuous frame update driving path advancement, yaw alignment, and running cycle.
     */
    fun update(deltaTimeSeconds: Float) {
        if (!isEvacuationActive || !isVisible) return
        val dt = deltaTimeSeconds.coerceIn(0.001f, 0.1f)
        totalElapsedSec += dt

        // Digital scanline sweep oscillation (1.8 Hz)
        val scanY = 0.85f + 0.65f * sin(totalElapsedSec * 4.2f)
        scanlineRing.position = Float3(0f, scanY, 0f)

        // Floating chevron bobbing and scale pulse
        val chevronBob = 1.95f + 0.05f * sin(totalElapsedSec * 5.0f)
        floatingChevronGroup.position = Float3(0f, chevronBob, 0f)

        // Subtle digital hologram glitch/flicker on light intensity
        val flicker = 1.0f + 0.12f * sin(totalElapsedSec * 35.0f) + 0.06f * sin(totalElapsedSec * 82.0f)
        val lightBase = if (isCompleted) BASE_LIGHT_INTENSITY * 1.6f else BASE_LIGHT_INTENSITY
        hologramLight?.intensity = lightBase * flicker

        // -------------------------------------------------------------
        // PHASE 1: SPAWN / MATERIALIZATION
        // -------------------------------------------------------------
        if (spawnTimer < SPAWN_DURATION) {
            spawnTimer += dt
            val t = (spawnTimer / SPAWN_DURATION).coerceIn(0f, 1f)
            // Elastic materialization expansion
            val s = sin(t * PI.toFloat() * 0.5f)
            rootRig.scale = Float3(s, s, s)

            // Step smoothly from the right wall onto the railway track centerline
            val startSample = navSystem.samplePath(currentProgress)
            val lerpX = 1.25f + (startSample.x - 1.25f) * t
            val lerpZ = -4.20f + (startSample.z - (-4.20f)) * t
            position = Float3(lerpX, startSample.yFloor, lerpZ)

            // Face forward down the tunnel
            currentYaw = -20f * (1f - t)
            rotation = Float3(0f, currentYaw, 0f)

            // Idle to initial jog transition
            animateRunKinematics(phase = t * 3.0f, intensity = t * 0.6f)
            return
        }

        // -------------------------------------------------------------
        // PHASE 2: ACTIVE RUNNING ALONG WAYPOINTS
        // -------------------------------------------------------------
        if (!isCompleted) {
            // Speed governor:
            // - Slow down slightly at junction (Z = -20m .. -26m, s = 19m .. 25m) so trainee sees hologram turn RIGHT past the sign
            // - Slow down smoothly as terminus exit is approached (s >= 56m)
            val targetSpeed = when {
                currentProgress in 19.0f..25.5f -> JUNCTION_APPROACH_SPEED
                currentProgress >= 56.0f -> TERMINUS_APPROACH_SPEED
                else -> NORMAL_RUN_SPEED
            }

            currentProgress += targetSpeed * dt
            runPhase += (targetSpeed * dt / STRIDE_LENGTH) * (2f * PI.toFloat())

            val maxPathS = navSystem.maxProgress - 0.25f
            if (currentProgress >= maxPathS) {
                currentProgress = maxPathS
                isCompleted = true
                completionTimer = 0f
                Log.i(tag, "Evacuation Hologram reached terminus evacuation exit at s = $currentProgress. Safe zone reached.")
                onEvacuationComplete?.invoke()
            }

            // Sample path position and forward orientation
            val sample = navSystem.samplePath(currentProgress)
            position = Float3(sample.x, sample.yFloor, sample.z)

            // Calculate forward yaw angle facing travel direction:
            // When moving along -Z (down tunnel), yaw = 0 deg.
            // When moving toward +X (turning right), yaw > 0 deg.
            val targetYaw = Math.toDegrees(
                atan2(sample.tangentX.toDouble(), (-sample.tangentZ).toDouble())
            ).toFloat()

            var deltaYaw = (targetYaw - currentYaw) % 360f
            if (deltaYaw > 180f) deltaYaw -= 360f
            if (deltaYaw < -180f) deltaYaw += 360f
            currentYaw += deltaYaw * (dt * 9.5f).coerceIn(0f, 1f)
            rotation = Float3(0f, currentYaw, 0f)

            // Drive full articulated running kinematics
            animateRunKinematics(phase = runPhase, intensity = 1.0f)
            return
        }

        // -------------------------------------------------------------
        // PHASE 3: COMPLETION / ARRIVAL AT SAFE ZONE
        // -------------------------------------------------------------
        completionTimer += dt

        // Transition from running pose to standing guide pose
        val blendToStand = (completionTimer * 2.0f).coerceIn(0f, 1f)
        animateCompletionKinematics(blendToStand)

        // Turn slightly to face approaching trainee
        val endYaw = currentYaw + 140f * blendToStand
        rotation = Float3(0f, endYaw, 0f)

        // Graceful fadeout after completion duration
        if (completionTimer > COMPLETION_DISPLAY_DURATION) {
            val fadeT = ((completionTimer - COMPLETION_DISPLAY_DURATION) / 1.5f).coerceIn(0f, 1f)
            val fadeScale = (1.0f - fadeT).coerceAtLeast(0.01f)
            rootRig.scale = Float3(fadeScale, fadeScale, fadeScale)
            if (fadeT >= 1.0f) {
                isVisible = false
            }
        }
    }

    /**
     * Procedural running kinematic simulation:
     * - Alternating hip swings with authentic back-swing knee flexion
     * - Arms counter-swinging opposite to leg motion with athletic elbow bend
     * - Vertical torso bounce and forward running lean
     */
    private fun animateRunKinematics(phase: Float, intensity: Float) {
        val sinP = sin(phase)
        val cosP = cos(phase)

        // 1. Torso lean & bounce
        // Forward lean into the run (8 - 11 deg)
        val torsoPitch = 10.0f * intensity
        // Double-frequency vertical bounce (runner lifts and drops twice per stride cycle)
        val torsoBounceY = 0.032f * abs(sinP) * intensity
        // Subtle torso sway
        val torsoYaw = 3.5f * sinP * intensity
        val torsoRoll = 2.0f * cosP * intensity

        torsoJoint.position = Float3(0f, 0.08f + torsoBounceY, 0f)
        torsoJoint.rotation = Float3(torsoPitch, torsoYaw, torsoRoll)

        // 2. Legs: Alternating hips & knees
        // Left hip: pitch forward (+) when sinP > 0, back (-) when sinP < 0
        val leftHipPitch = 38.0f * sinP * intensity
        val rightHipPitch = -38.0f * sinP * intensity

        leftHipJoint.rotation = Float3(leftHipPitch, 0f, 0f)
        rightHipJoint.rotation = Float3(rightHipPitch, 0f, 0f)

        // Knee flexion:
        // When a leg swings BACKWARD, the knee flexes sharply upward (up to 55 deg).
        // When swinging FORWARD, the knee extends straight (5 deg slight bend).
        val leftKneeBend = if (sinP < 0f) {
            (abs(sinP) * 52.0f + 5.0f) * intensity
        } else {
            5.0f * intensity
        }

        val rightKneeBend = if (sinP > 0f) {
            (abs(sinP) * 52.0f + 5.0f) * intensity
        } else {
            5.0f * intensity
        }

        leftKneeJoint.rotation = Float3(leftKneeBend, 0f, 0f)
        rightKneeJoint.rotation = Float3(rightKneeBend, 0f, 0f)

        // 3. Arms: Counter-swinging opposite to legs
        // When left leg is forward (sinP > 0), left arm swings BACKWARD
        val leftShoulderPitch = -32.0f * sinP * intensity
        val rightShoulderPitch = 32.0f * sinP * intensity

        leftShoulderJoint.rotation = Float3(leftShoulderPitch, 0f, -6.0f)
        rightShoulderJoint.rotation = Float3(rightShoulderPitch, 0f, 6.0f)

        // Elbows remain bent in athletic runner posture (~65 deg) with rhythmic pumping (+/- 14 deg)
        val leftElbowBend = (65.0f + 14.0f * cosP) * intensity
        val rightElbowBend = (65.0f - 14.0f * cosP) * intensity

        leftElbowJoint.rotation = Float3(leftElbowBend, 0f, 0f)
        rightElbowJoint.rotation = Float3(rightElbowBend, 0f, 0f)
    }

    /**
     * Standing guide pose at the evacuation exit:
     * Right hand gestures outward toward the exit drift opening.
     */
    private fun animateCompletionKinematics(blend: Float) {
        val b = blend.coerceIn(0f, 1f)
        val invB = 1f - b

        // Torso upright
        torsoJoint.position = Float3(0f, 0.08f, 0f)
        torsoJoint.rotation = Float3(10f * invB, 0f, 0f)

        // Legs straight
        leftHipJoint.rotation = Float3(0f, 0f, 0f)
        rightHipJoint.rotation = Float3(0f, 0f, 0f)
        leftKneeJoint.rotation = Float3(3f, 0f, 0f)
        rightKneeJoint.rotation = Float3(3f, 0f, 0f)

        // Left arm resting at side
        leftShoulderJoint.rotation = Float3(0f, 0f, -8f)
        leftElbowJoint.rotation = Float3(18f, 0f, 0f)

        // Right arm raised, gesturing toward the evacuation exit
        val gesturePitch = -45f * b
        val gestureYaw = -25f * b
        val gestureRoll = 35f * b
        rightShoulderJoint.rotation = Float3(gesturePitch, gestureYaw, gestureRoll)
        rightElbowJoint.rotation = Float3(30f * b, 0f, 0f)
    }

    /**
     * Sceneview frame callback hook.
     */
    override fun onFrame(frameTimeNanos: Long) {
        super.onFrame(frameTimeNanos)
        if (lastFrameTimeNanos > 0L) {
            val dt = ((frameTimeNanos - lastFrameTimeNanos).toDouble() / 1_000_000_000.0).toFloat()
            update(dt)
        }
        lastFrameTimeNanos = frameTimeNanos
    }

    fun reset() {
        isEvacuationActive = false
        isCompleted = false
        currentProgress = 0f
        spawnTimer = 0f
        completionTimer = 0f
        totalElapsedSec = 0f
        lastFrameTimeNanos = -1L
        isVisible = false
        rootRig.scale = Float3(0.01f, 0.01f, 0.01f)
    }

    override fun destroy() {
        isVisible = false
        hologramLight?.destroy()
        hologramLight = null
        super.destroy()
        Log.i(tag, "EvacuationHologram destroyed")
    }
}
