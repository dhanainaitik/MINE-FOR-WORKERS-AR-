package com.minesafe.ar.ui

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult as ArHitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import androidx.compose.ui.platform.LocalContext
import com.minesafe.ar.R
import com.minesafe.ar.ar.ChemicalHazardNode
import com.minesafe.ar.ar.ChemicalKitNode
import com.minesafe.ar.ar.DoorwayNode
import com.minesafe.ar.ar.ElectricalBoxNode
import com.minesafe.ar.ar.VideoFireNode
import com.minesafe.ar.ar.EmergencyStationNode
import com.minesafe.ar.ar.ExtinguisherHoldingState
import com.minesafe.ar.ar.ExtinguisherNode
import com.minesafe.ar.ar.FireNode
import com.minesafe.ar.ar.MineEnvironmentNode
import com.minesafe.ar.ar.PlacementReticleNode
import com.minesafe.ar.gestures.HandGestureState
import com.minesafe.ar.gestures.HandTrackingManager
import com.minesafe.ar.audio.AudioManager
import com.minesafe.ar.audio.VoiceInstructionManager
import com.minesafe.ar.training.TrainingModule
import com.minesafe.ar.training.TrainingState
import com.minesafe.ar.training.TrainingViewModel
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.HitResult
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MainApp(
    viewModel: TrainingViewModel,
    voiceManager: VoiceInstructionManager,
    audioManager: AudioManager,
    onTrainingComplete: () -> Unit
) {
    val context = LocalContext.current
    val selectedModule by viewModel.selectedModule.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val score by viewModel.score.collectAsState()
    val timeSeconds by viewModel.timeSeconds.collectAsState()
    val isSoundMuted by viewModel.isSoundMuted.collectAsState()
    val nozzleReminderCount by viewModel.nozzleReminderTrigger.collectAsState()

    var showHelpModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val modelLoader = rememberModelLoader(engine)

    // Materials calibrated for authentic underground mine rendering
    val rockMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF2E2925), 0.05f, 0.85f, 0.35f) }
    val floorMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF262220), 0.05f, 0.90f, 0.30f) }
    val darkMetalMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF424242), 0.70f, 0.40f, 0.60f) }
    val reticleWhiteMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFFFFFF), 0.0f, 0.10f, 1.0f) }
    val hazardYellowMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFFD600), 0.1f, 0.40f, 0.4f) }
    val hazardBandMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF263238), 0.1f, 0.50f, 0.3f) }
    val pipeMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF546E7A), 0.75f, 0.35f, 0.5f) }
    val fireCoreMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFFF9C4), 0.0f, 0.05f, 1.0f) }
    val fireOuterMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFF3D00), 0.0f, 0.10f, 1.0f) }
    val smokeMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF37474F), 0.0f, 0.90f, 0.10f) }
    val extRedMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFE53935), 0.15f, 0.30f, 0.4f) }
    val brassMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFFCA28), 0.85f, 0.25f, 0.7f) }
    val rubberMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF212121), 0.0f, 0.80f, 0.2f) }
    val sprayMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFECEFF1), 0.0f, 0.80f, 0.2f) }
    val greenGaugeMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF00E676), 0.0f, 0.20f, 0.6f) }
    val chemVaporMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFCCFF90), 0.0f, 0.10f, 0.8f) }
    val chemPuddleMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF76FF03), 0.1f, 0.15f, 0.9f) }
    val greenStationMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF2E7D32), 0.1f, 0.40f, 0.4f) }
    val whiteMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFFFFFFF), 0.0f, 0.30f, 0.5f) }
    val blackDoorwayMat = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF141414), 0.15f, 0.35f, 0.2f) }

    // AR Placement Reticle for horizontal floor detection
    val reticleNode = remember(engine, reticleWhiteMat) {
        PlacementReticleNode(engine, reticleWhiteMat).apply {
            isVisible = false
        }
    }

    var currentFloorHit by remember { mutableStateOf<ArHitResult?>(null) }
    var childNodes by remember { mutableStateOf(listOf<Node>(reticleNode)) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var doorwayAnchor by remember { mutableStateOf<Anchor?>(null) }
    var anchorYawRad by remember { mutableFloatStateOf(0f) }
    var doorDistance by remember { mutableFloatStateOf(-1f) }
    var trackingLost by remember { mutableStateOf(false) }
    var detectedPlanesCount by remember { mutableIntStateOf(0) }
    var debugPlacementStatus by remember { mutableStateOf("SCANNING FLOOR") }

    // Virtual Mine Container Node: Holds the mine and shifts along +Z for 10x virtual walking amplification
    var virtualMineContainer by remember { mutableStateOf<Node?>(null) }

    // Hazard and Equipment Node references
    var electricalBoxNode by remember { mutableStateOf<ElectricalBoxNode?>(null) }
    var videoFireNode by remember { mutableStateOf<VideoFireNode?>(null) }
    var fireNode by remember { mutableStateOf<FireNode?>(null) }
    var extinguisherNode by remember { mutableStateOf<ExtinguisherNode?>(null) }
    var chemicalNode by remember { mutableStateOf<ChemicalHazardNode?>(null) }
    var chemicalKitNode by remember { mutableStateOf<ChemicalKitNode?>(null) }
    var emergencyStationNode by remember { mutableStateOf<EmergencyStationNode?>(null) }

    var isAimingAtFire by remember { mutableStateOf(false) }
    var dangerZoneWarningTriggered by remember { mutableStateOf(false) }
    val sweepProgress by viewModel.sweepProgress.collectAsState()
    var aimAlignedDurationMs by remember { mutableLongStateOf(0L) }
    var accumulatedSweepAngle by remember { mutableFloatStateOf(0f) }
    var lastHorizontalAimAngle by remember { mutableFloatStateOf(0f) }
    var lastWarningVoiceTimeMs by remember { mutableLongStateOf(0L) }
    var isAimAlignedWithFireBase by remember { mutableStateOf(false) }
    var stableHandX by remember { mutableFloatStateOf(0.5f) }
    var stableHandY by remember { mutableFloatStateOf(0.5f) }
    var stableYaw by remember { mutableFloatStateOf(0f) }

    // MediaPipe Hand Gesture Tracking (Module 1: Electrical Fire Safety)
    var handGestureState by remember { mutableStateOf(HandGestureState()) }
    val handTrackingManager = remember(context) {
        HandTrackingManager(context) { state ->
            handGestureState = state
        }
    }
    DisposableEffect(handTrackingManager) {
        onDispose {
            handTrackingManager.destroy()
        }
    }

    // Directional illumination down the mine drift
    val mainLight = rememberMainLightNode(engine) {
        color = Float4(1.0f, 0.95f, 0.85f, 1.0f)
        intensity = 160000f
        lightDirection = Float3(0.15f, -0.75f, -0.65f)
    }

    // Camera node with extended far plane (150m) so that the entire authentic mine environment,
    // including the curved section and the left-side continuation, remains 100% visible and unclipped.
    val cameraNode = rememberARCameraNode(engine) {
        ARSceneView.createARCameraNode(engine).apply {
            far = 150.0f
        }
    }

    // Periodic nozzle reminder reaction
    LaunchedEffect(nozzleReminderCount) {
        if (nozzleReminderCount > 0 && currentState == TrainingState.EXTINGUISHER_REACHED) {
            voiceManager.speak("Please open the extinguisher nozzle.")
        }
    }

    // Module-scoped hazard lifecycle management:
    // Ensures Module 02 (Chemical Hazard) never retains, loads, or plays fire assets/effects
    LaunchedEffect(selectedModule) {
        if (selectedModule == TrainingModule.CHEMICAL_HAZARD) {
            handTrackingManager.isEnabled = false
            videoFireNode?.let { node ->
                node.stopFire()
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            videoFireNode = null

            electricalBoxNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            electricalBoxNode = null

            extinguisherNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            extinguisherNode = null

            audioManager.stopHazardSound()

            val container = virtualMineContainer
            if (container != null) {
                if (chemicalKitNode == null) {
                    val kit = ChemicalKitNode(
                        engine = engine,
                        modelLoader = modelLoader
                    ).apply {
                        position = Float3(-1.75f, 0.00f, -6.00f)
                    }
                    container.addChildNode(kit)
                    chemicalKitNode = kit
                }
                if (chemicalNode == null) {
                    val chem = ChemicalHazardNode(
                        engine = engine,
                        modelLoader = modelLoader,
                        vaporMaterial = chemVaporMat,
                        puddleMaterial = chemPuddleMat,
                        perimeterMaterial = hazardYellowMat
                    ).apply {
                        position = Float3(0.00f, 0.00f, 0.00f)
                    }
                    container.addChildNode(chem)
                    chemicalNode = chem
                }
            }
        } else if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
            handTrackingManager.isEnabled = true
            chemicalKitNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            chemicalKitNode = null

            chemicalNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            chemicalNode = null

            val container = virtualMineContainer
            if (container != null) {
                if (electricalBoxNode == null) {
                    val box = ElectricalBoxNode(
                        engine = engine,
                        modelLoader = modelLoader
                    ).apply {
                        position = Float3(1.95f, 0.82f, -10.20f)
                        rotation = Float3(0.0f, -135.0f, 0.0f)
                    }
                    container.addChildNode(box)
                    electricalBoxNode = box
                }
                if (videoFireNode == null) {
                    val fireVideo = VideoFireNode(
                        engine = engine,
                        materialLoader = materialLoader,
                        context = context,
                        planeSize = Float3(2.50f, 1.40f, 0.0f)
                    ).apply {
                        position = Float3(1.80f, 1.14f, -10.05f)
                        rotation = Float3(0.0f, -45.0f, 0.0f)
                    }
                    container.addChildNode(fireVideo)
                    videoFireNode = fireVideo
                    fireVideo.startFire()
                }
                if (extinguisherNode == null) {
                    val ext = ExtinguisherNode(
                        engine = engine,
                        modelLoader = modelLoader,
                        brassMaterial = brassMat,
                        leverMaterial = extRedMat,
                        sprayMaterial = sprayMat
                    ).apply {
                        position = Float3(-1.50f, 0.00f, -6.50f)
                    }
                    container.addChildNode(ext)
                    extinguisherNode = ext
                }
            }
        }
    }

    // Cleanup on leaving training screen
    DisposableEffect(Unit) {
        onDispose {
            videoFireNode?.let { node ->
                node.stopFire()
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            videoFireNode = null

            electricalBoxNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            electricalBoxNode = null

            extinguisherNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            extinguisherNode = null

            chemicalKitNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            chemicalKitNode = null

            chemicalNode?.let { node ->
                node.isVisible = false
                virtualMineContainer?.removeChildNode(node)
                node.destroy()
            }
            chemicalNode = null

            audioManager.stopHazardSound()
            audioManager.stopMineAmbiance()
        }
    }

    // Voice instruction and sound reaction based on state changes
    LaunchedEffect(currentState) {
        when (currentState) {
            TrainingState.START, TrainingState.SCAN_FLOOR -> {
                voiceManager.speak("Scan the floor to place the mine entrance.")
            }
            TrainingState.PLACE_DOORWAY -> {
                voiceManager.speak("Scan the floor to place the mine entrance.")
            }
            TrainingState.ENTER_MINE -> {
                voiceManager.speak("Physically walk toward the doorway to enter the mine.")
            }
            // Module 1: Electrical Fire (Sequential PASS Voice Guidance)
            TrainingState.FIRE_DETECTED -> {
                videoFireNode?.startFire()
                audioManager.startMineAmbiance()
                audioManager.startFireSound()
                voiceManager.speak("Warning. Electrical fire detected ahead. Move toward the fire extinguisher.")
            }
            TrainingState.EXTINGUISHER_REACHED -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Pinch your thumb and index finger to pick up the fire extinguisher.")
            }
            TrainingState.EXTINGUISHER_HELD -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Extinguisher acquired! Approach the electrical fire.")
            }
            TrainingState.PULL_SAFETY_PIN -> {
                voiceManager.speak("Pull the safety pin.")
            }
            TrainingState.AIM_AT_FIRE_BASE, TrainingState.AIM_AT_FIRE, TrainingState.OPEN_NOZZLE -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Aim the nozzle at the base of the fire.")
            }
            TrainingState.SQUEEZE_LEVER -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Now squeeze the lever.")
            }
            TrainingState.SWEEP_SIDE_TO_SIDE -> {
                voiceManager.speak("Sweep the nozzle from side to side across the base of the fire.")
            }
            TrainingState.FIRE_EXTINGUISHED -> {
                videoFireNode?.stopFire()
                audioManager.stopHazardSound()
                audioManager.playSuccessChime()
                voiceManager.speak("Fire extinguished. Good job following safety procedures.")
                delay(3000)
                onTrainingComplete()
            }
            // Module 2: Chemical Hazard
            TrainingState.CHEMICAL_HAZARD_DETECTED -> {
                audioManager.startMineAmbiance()
                audioManager.startChemicalLeakSound()
                voiceManager.speak("Chemical hazard detected. Do not approach the leak.")
            }
            TrainingState.MAINTAIN_SAFE_DISTANCE -> {
                voiceManager.speak("Move away from the hazard zone.")
            }
            TrainingState.LEAVE_HAZARD_ZONE -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Locate the emergency response equipment.")
            }
            TrainingState.LOCATE_EMERGENCY_EQUIPMENT -> {
                voiceManager.speak("Do not touch the leaking container. Follow the emergency procedure.")
            }
            TrainingState.PERFORM_SAFE_RESPONSE -> {
                voiceManager.speak("Activate the emergency isolation valve to seal the line.")
            }
            TrainingState.HAZARD_CONTROLLED -> {
                audioManager.stopHazardSound()
                audioManager.playSuccessChime()
                voiceManager.speak("Hazard controlled. Emergency isolation confirmed.")
                delay(3000)
                onTrainingComplete()
            }
            else -> {}
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cx = if (constraints.maxWidth > 0) constraints.maxWidth.toFloat() / 2f else 540f
        val cy = if (constraints.maxHeight > 0) constraints.maxHeight.toFloat() / 2f else 960f

        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            mainLightNode = mainLight,
            cameraNode = cameraNode,
            childNodes = childNodes,
            planeRenderer = (doorwayAnchor == null),
            sessionConfiguration = { _, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.focusMode = Config.FocusMode.AUTO
                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
            },
            onTouchEvent = { e: MotionEvent, hitResult: HitResult? ->
                if (e.action == MotionEvent.ACTION_UP) {
                    // Tap to place Black AR Doorway on detected floor
                    if (doorwayAnchor == null && (currentState == TrainingState.START || currentState == TrainingState.SCAN_FLOOR || currentState == TrainingState.PLACE_DOORWAY)) {
                        val frame = currentFrame
                        if (frame != null) {
                            var tapHit = frame.hitTest(e.x, e.y).firstOrNull { hit ->
                                val trackable = hit.trackable
                                trackable is Plane &&
                                trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                trackable.trackingState == TrackingState.TRACKING
                            }
                            if (tapHit == null) {
                                tapHit = currentFloorHit ?: frame.hitTest(cx, cy).firstOrNull { hit ->
                                    val trackable = hit.trackable
                                    trackable is Plane &&
                                    trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                    trackable.trackingState == TrackingState.TRACKING
                                }
                            }
                            if (tapHit == null) {
                                tapHit = frame.hitTest(cx, cy * 1.15f).firstOrNull { hit ->
                                    val trackable = hit.trackable
                                    trackable is Plane &&
                                    trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                    trackable.trackingState == TrackingState.TRACKING
                                }
                            }

                            if (tapHit != null) {
                                val hitPose = tapHit.hitPose
                                val camPose = frame.camera.pose
                                val dx = camPose.tx() - hitPose.tx()
                                val dz = camPose.tz() - hitPose.tz()
                                val yawRad = Math.atan2(dx.toDouble(), dz.toDouble()).toFloat()
                                anchorYawRad = yawRad
                                val qy = Math.sin(yawRad / 2.0).toFloat()
                                val qw = Math.cos(yawRad / 2.0).toFloat()
                                val orientedPose = Pose(
                                    floatArrayOf(hitPose.tx(), hitPose.ty(), hitPose.tz()),
                                    floatArrayOf(0f, qy, 0f, qw)
                                )
                                val anchor = (tapHit.trackable as? Plane)?.createAnchor(orientedPose)
                                    ?: tapHit.createAnchorOrNull()

                                if (anchor != null) {
                                    doorwayAnchor = anchor
                                    reticleNode.isVisible = false
                                    val anchorNode = AnchorNode(engine, anchor)

                                    // 1. Black AR Doorway Frame (Stands vertically, anchored to real floor)
                                    val doorwayNode = DoorwayNode(engine, frameMaterial = blackDoorwayMat)
                                    anchorNode.addChildNode(doorwayNode)

                                    // 2. Virtual Mine Container (Carries the authentic GLB and props for 10x movement amplification)
                                    val mineContainer = Node(engine)
                                    anchorNode.addChildNode(mineContainer)
                                    virtualMineContainer = mineContainer

                                    // 3. Exact Underground Mine Model: AR_Mine_Optimized.glb + Subterranean Enclosure
                                    val mineEnv = MineEnvironmentNode(
                                        engine = engine,
                                        modelLoader = modelLoader,
                                        rockMaterial = rockMat,
                                        floorMaterial = floorMat,
                                        darkRockMaterial = rockMat
                                    )
                                    mineContainer.addChildNode(mineEnv)

                                    // 4. Module-Scoped Hazard:
                                    // Module 01: Electrical Fire Safety -> Wall-Mounted Electrical Box + Looping MP4 Fire Video
                                    // Module 02: Chemical Hazard Response -> NO fire/box asset, NO video, NO fire effects
                                    if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                                        // 1. Industrial Electrical Box mounted on right mine wall / timber arch
                                        val box = ElectricalBoxNode(
                                            engine = engine,
                                            modelLoader = modelLoader
                                        ).apply {
                                            // Wall-mounted flush against right mine wall / wooden support arch at chest height
                                            position = Float3(1.95f, 0.82f, -10.20f)
                                            rotation = Float3(0.0f, -135.0f, 0.0f)
                                        }
                                        mineContainer.addChildNode(box)
                                        electricalBoxNode = box

                                        // 2. Video Fire Hazard Node: MP4 fire video directly in front of the electrical box
                                        val fireVideo = VideoFireNode(
                                            engine = engine,
                                            materialLoader = materialLoader,
                                            context = context,
                                            planeSize = Float3(2.50f, 1.40f, 0.0f)
                                        ).apply {
                                            position = Float3(1.80f, 1.14f, -10.05f)
                                            rotation = Float3(0.0f, -45.0f, 0.0f)
                                        }
                                        mineContainer.addChildNode(fireVideo)
                                        videoFireNode = fireVideo

                                        // 3. Fire Extinguisher: Standing upright on mine floor on the LEFT side of the walking path before the fire
                                        val ext = ExtinguisherNode(
                                            engine = engine,
                                            modelLoader = modelLoader,
                                            brassMaterial = brassMat,
                                            leverMaterial = extRedMat,
                                            sprayMaterial = sprayMat
                                        ).apply {
                                            position = Float3(-1.50f, 0.00f, -6.50f)
                                        }
                                        mineContainer.addChildNode(ext)
                                        extinguisherNode = ext
                                    } else {
                                        electricalBoxNode = null
                                        videoFireNode = null
                                        extinguisherNode = null

                                        // ASSET 1: Chemical Kit on LEFT side of virtual walking path before hazard
                                        val kit = ChemicalKitNode(
                                            engine = engine,
                                            modelLoader = modelLoader
                                        ).apply {
                                            position = Float3(-1.75f, 0.00f, -6.00f)
                                        }
                                        mineContainer.addChildNode(kit)
                                        chemicalKitNode = kit

                                        // ASSETS 2 & 3: Chemical Hazard Area (chemical_tank.glb + metal_barrel.glb)
                                        val chem = ChemicalHazardNode(
                                            engine = engine,
                                            modelLoader = modelLoader,
                                            vaporMaterial = chemVaporMat,
                                            puddleMaterial = chemPuddleMat,
                                            perimeterMaterial = hazardYellowMat
                                        ).apply {
                                            position = Float3(0.00f, 0.00f, 0.00f)
                                        }
                                        mineContainer.addChildNode(chem)
                                        chemicalNode = chem
                                    }

                                    childNodes = (childNodes - reticleNode) + anchorNode
                                    viewModel.updateState(TrainingState.ENTER_MINE)
                                    audioManager.playSuccessChime()
                                    voiceManager.speak("Physically walk toward the doorway to enter the mine.")
                                    return@ARScene true
                                }
                            }
                        }
                    }

                    // Interactive touch actions on 3D equipment (Pickup is strictly gesture-driven via thumb-index pinch)
                    when (currentState) {
                        TrainingState.LOCATE_EMERGENCY_EQUIPMENT -> {
                            if (hitResult?.node == chemicalKitNode || hitResult?.node == chemicalKitNode?.modelNode || hitResult?.node == chemicalKitNode?.kitInteractiveTarget) {
                                chemicalKitNode?.equipKit()
                                viewModel.updateState(TrainingState.PERFORM_SAFE_RESPONSE)
                            }
                        }
                        TrainingState.PERFORM_SAFE_RESPONSE -> {
                            if (hitResult?.node == emergencyStationNode?.isolationValve || hitResult?.node == emergencyStationNode?.isolationLever || hitResult?.node == chemicalNode || hitResult?.node == chemicalNode?.tankModelNode) {
                                emergencyStationNode?.activateValve()
                                chemicalNode?.setHazardControlled(true)
                                viewModel.updateState(TrainingState.HAZARD_CONTROLLED)
                            }
                        }
                        else -> {}
                    }
                }
                false
            },
            onSessionUpdated = { session, frame ->
                currentFrame = frame
                val mainHandler = Handler(Looper.getMainLooper())
                val isTracking = frame.camera.trackingState == TrackingState.TRACKING

                val allPlanes = session.getAllTrackables(Plane::class.java)
                val horizontalPlanes = allPlanes.filter {
                    it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.trackingState == TrackingState.TRACKING
                }
                val planeCount = horizontalPlanes.size

                mainHandler.post {
                    trackingLost = !isTracking
                    detectedPlanesCount = planeCount
                }

                // Floor Scanning Reticle Logic
                if (doorwayAnchor == null && isTracking) {
                    val hits = frame.hitTest(cx, cy)
                    val floorHit = hits.firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane &&
                        trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        trackable.trackingState == TrackingState.TRACKING
                    } ?: hits.firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane && trackable.trackingState == TrackingState.TRACKING
                    } ?: frame.hitTest(cx, cy * 1.15f).firstOrNull { hit ->
                        val trackable = hit.trackable
                        trackable is Plane &&
                        trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        trackable.trackingState == TrackingState.TRACKING
                    }

                    if (floorHit != null) {
                        currentFloorHit = floorHit
                        val hitPose = floorHit.hitPose
                        reticleNode.position = Float3(hitPose.tx(), hitPose.ty(), hitPose.tz())
                        val camPose = frame.camera.pose
                        val dx = camPose.tx() - hitPose.tx()
                        val dz = camPose.tz() - hitPose.tz()
                        val yawDeg = Math.toDegrees(Math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
                        reticleNode.rotation = Float3(0f, yawDeg, 0f)
                        reticleNode.isVisible = true

                        mainHandler.post {
                            debugPlacementStatus = "FLOOR DETECTED — TAP TO PLACE"
                        }
                    } else if (planeCount > 0) {
                        mainHandler.post {
                            debugPlacementStatus = "FLOOR DETECTED ($planeCount) — POINT AT FLOOR"
                        }
                    } else {
                        reticleNode.isVisible = false
                        currentFloorHit = null
                        mainHandler.post {
                            debugPlacementStatus = "SCANNING FLOOR"
                        }
                    }
                } else if (doorwayAnchor != null) {
                    mainHandler.post {
                        debugPlacementStatus = "MINE ACTIVE"
                    }
                }

                val cameraPose = frame.camera.pose
                val dAnchor = doorwayAnchor

                if (dAnchor != null) {
                    // Calculate camera position in anchor local coordinates
                    val anchorPose = dAnchor.pose
                    val dx = cameraPose.tx() - anchorPose.tx()
                    val dy = cameraPose.ty() - anchorPose.ty()
                    val dz = cameraPose.tz() - anchorPose.tz()

                    val cosTheta = Math.cos(anchorYawRad.toDouble()).toFloat()
                    val sinTheta = Math.sin(anchorYawRad.toDouble()).toFloat()

                    val localCamX = cosTheta * dx - sinTheta * dz
                    val localCamZ = sinTheta * dx + cosTheta * dz

                    mainHandler.post {
                        doorDistance = if (localCamZ > 0) localCamZ else 0f
                    }

                    // --- ENTERING THE MINE DETECTION ---
                    if (currentState == TrainingState.ENTER_MINE && localCamZ <= 1.25f) {
                        mainHandler.post {
                            if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                                viewModel.updateState(TrainingState.FIRE_DETECTED)
                            } else {
                                viewModel.updateState(TrainingState.CHEMICAL_HAZARD_DETECTED)
                            }
                        }
                    }

                    // =========================================================================
                    // 10X MOVEMENT AMPLIFICATION (Requirements 8 & 9)
                    // 1 metre of physical forward walking = 10 metres of virtual mine movement
                    // =========================================================================
                    // When worker crosses the doorway (localCamZ < 0):
                    // Physical penetration depth: p = -localCamZ
                    // Virtual penetration: V = 10 * p
                    // Virtual mine container shifts along +Z by Delta = 9 * p
                    val p = (-localCamZ).coerceAtLeast(0f)
                    val virtualAdvance = p * 10f
                    val mineShiftZ = p * 9f

                    virtualMineContainer?.position = Float3(0f, 0f, mineShiftZ)

                    val workerVirtualZ = -virtualAdvance
                    val workerVirtualX = localCamX

                    // --- MODULE 1: FIRE & EXTINGUISHER PROXIMITY & GESTURE INTERACTION ---
                    if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                        handTrackingManager.processFrame(frame)

                        val extVirtualZ = -6.50f
                        val extVirtualX = -1.50f
                        val distToExt = Math.hypot(
                            (workerVirtualX - extVirtualX).toDouble(),
                            (workerVirtualZ - extVirtualZ).toDouble()
                        ).toFloat()

                        val fireVirtualZ = -10.20f
                        val fireVirtualX = 1.95f
                        val distToFire = Math.hypot(
                            (workerVirtualX - fireVirtualX).toDouble(),
                            (workerVirtualZ - fireVirtualZ).toDouble()
                        ).toFloat()

                        val isExtHeld = extinguisherNode?.holdingState == ExtinguisherHoldingState.HELD

                        mainHandler.post {
                            viewModel.updateDistanceToObjective(if (!isExtHeld) distToExt else distToFire)
                            isAimingAtFire = isExtHeld && (distToFire <= 4.5f)
                        }

                        // Transition: Approach extinguisher
                        if (!isExtHeld && distToExt <= 2.2f && (currentState == TrainingState.FIRE_DETECTED || currentState == TrainingState.GO_TO_EXTINGUISHER)) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.EXTINGUISHER_REACHED)
                            }
                        }

                        // Calculate camera forward heading in mine space
                        val forwardX = -cameraPose.zAxis[0]
                        val forwardY = -cameraPose.zAxis[1]
                        val forwardZ = -cameraPose.zAxis[2]
                        val cosT = Math.cos(anchorYawRad.toDouble()).toFloat()
                        val sinT = Math.sin(anchorYawRad.toDouble()).toFloat()
                        val localFwdX = cosT * forwardX - sinT * forwardZ
                        val localFwdY = forwardY
                        val localFwdZ = sinT * forwardX + cosT * forwardZ

                        // Horizontal unit forward vector
                        val fwdLen = Math.hypot(localFwdX.toDouble(), localFwdZ.toDouble()).toFloat().coerceAtLeast(0.001f)
                        val uFwdX = localFwdX / fwdLen
                        val uFwdZ = localFwdZ / fwdLen

                        // Horizontal unit right vector: (uFwdZ, 0, -uFwdX)
                        val uRightX = uFwdZ
                        val uRightZ = -uFwdX

                        // Smooth hand tracking with low-pass filter (eliminates frame-dropping snap)
                        if (handGestureState.isHandPresent) {
                            stableHandX += (handGestureState.pinchCenterX - stableHandX) * 0.25f
                            stableHandY += (handGestureState.pinchCenterY - stableHandY) * 0.25f
                        } else {
                            stableHandX += (0.5f - stableHandX) * 0.03f
                            stableHandY += (0.5f - stableHandY) * 0.03f
                        }

                        // Interaction 0: Pick Up Extinguisher with Pinch (debounced hold >= 80ms)
                        if (!isExtHeld && distToExt <= 2.5f && (currentState == TrainingState.EXTINGUISHER_REACHED || currentState == TrainingState.FIRE_DETECTED)) {
                            if (handGestureState.isPinchPickupTriggered()) {
                                val targetYawDeg = Math.toDegrees(Math.atan2((-uFwdX).toDouble(), (-uFwdZ).toDouble())).toFloat()
                                stableYaw = targetYawDeg
                                mainHandler.post {
                                    extinguisherNode?.setHeld(true)
                                    viewModel.updateState(TrainingState.EXTINGUISHER_HELD)
                                    audioManager.playSuccessChime()
                                }
                            }
                        }

                        // Extinguisher HELD position tracking:
                        // Positioned stably in front of worker, moving smoothly with camera and filtered hand
                        if (isExtHeld) {
                            val ext = extinguisherNode
                            if (ext != null) {
                                val handOffsetX = (stableHandX - 0.5f) * 0.22f
                                val handOffsetY = -(stableHandY - 0.5f) * 0.18f

                                val targetX = workerVirtualX + (uFwdX * 0.52f) + (uRightX * (0.16f + handOffsetX))
                                val targetY = 0.48f + handOffsetY
                                val targetZ = workerVirtualZ + (uFwdZ * 0.52f) + (uRightZ * (0.16f + handOffsetX))

                                val curPos = ext.position
                                ext.position = Float3(
                                    curPos.x + (targetX - curPos.x) * 0.25f,
                                    curPos.y + (targetY - curPos.y) * 0.25f,
                                    curPos.z + (targetZ - curPos.z) * 0.25f
                                )

                                val targetYawDeg = Math.toDegrees(Math.atan2((-uFwdX).toDouble(), (-uFwdZ).toDouble())).toFloat()
                                var deltaYaw = (targetYawDeg - stableYaw) % 360f
                                if (deltaYaw > 180f) deltaYaw -= 360f
                                if (deltaYaw < -180f) deltaYaw += 360f
                                stableYaw += deltaYaw * 0.20f
                                ext.rotation = Float3(0f, stableYaw, 0f)
                            }

                            // Advance to Step 1 (PULL_SAFETY_PIN) when worker approaches the fire with extinguisher
                            if (distToFire <= 4.5f && currentState == TrainingState.EXTINGUISHER_HELD) {
                                mainHandler.post {
                                    viewModel.updateState(TrainingState.PULL_SAFETY_PIN)
                                }
                            }

                            // Compute nozzle / camera aim angle relative to fire base (1.80f, 0.40f, -10.05f)
                            val toFireX = 1.80f - workerVirtualX
                            val toFireY = 0.40f - 1.25f
                            val toFireZ = -10.05f - workerVirtualZ
                            val toFireLen = Math.sqrt((toFireX * toFireX + toFireY * toFireY + toFireZ * toFireZ).toDouble()).toFloat()
                            val nFireX = toFireX / toFireLen.coerceAtLeast(0.01f)
                            val nFireY = toFireY / toFireLen.coerceAtLeast(0.01f)
                            val nFireZ = toFireZ / toFireLen.coerceAtLeast(0.01f)

                            val dotAim = (localFwdX * nFireX + localFwdY * nFireY + localFwdZ * nFireZ).coerceIn(-1f, 1f)
                            val aimAngleDeg = Math.toDegrees(Math.acos(dotAim.toDouble())).toFloat()
                            val isAimed = aimAngleDeg <= 32.0f

                            mainHandler.post {
                                isAimAlignedWithFireBase = isAimed
                            }

                            val isSqueezing = handGestureState.isLeverSqueezeActive()
                            val isPinOut = ext?.isPinRemoved == true
                            val now = SystemClock.uptimeMillis()

                            // Update 3D lever visual depression based on squeeze gesture
                            ext?.setLeverSqueezed(isSqueezing && isPinOut)

                            // --- STEP 1: PULL SAFETY PIN ---
                            if (currentState == TrainingState.PULL_SAFETY_PIN) {
                                if (handGestureState.isPinPullTriggered()) {
                                    mainHandler.post {
                                        ext?.triggerPinRemoval()
                                        audioManager.playPinPullSound()
                                        viewModel.updateState(TrainingState.AIM_AT_FIRE_BASE)
                                        voiceManager.speak("Safety pin removed! Now aim the nozzle at the base of the fire.")
                                    }
                                } else if (isSqueezing && now - lastWarningVoiceTimeMs > 3500L) {
                                    // Squeeze locked before pin removal
                                    lastWarningVoiceTimeMs = now
                                    mainHandler.post {
                                        audioManager.playMistakeBuzzer()
                                        viewModel.registerMistake(2)
                                        voiceManager.speak("Pull the safety pin first.")
                                    }
                                }
                            }

                            // --- STEP 2: AIM AT BASE OF FIRE ---
                            if (currentState == TrainingState.AIM_AT_FIRE_BASE || currentState == TrainingState.AIM_AT_FIRE || currentState == TrainingState.OPEN_NOZZLE) {
                                if (isAimed) {
                                    aimAlignedDurationMs += 40L
                                    if (aimAlignedDurationMs >= 500L) {
                                        mainHandler.post {
                                            audioManager.playSuccessChime()
                                            viewModel.updateState(TrainingState.SQUEEZE_LEVER)
                                            voiceManager.speak("Aim correct! Now squeeze the lever.")
                                        }
                                    }
                                } else {
                                    aimAlignedDurationMs = 0L
                                    if (isSqueezing && now - lastWarningVoiceTimeMs > 3500L) {
                                        // Squeeze locked before correct aiming
                                        lastWarningVoiceTimeMs = now
                                        mainHandler.post {
                                            audioManager.playMistakeBuzzer()
                                            viewModel.registerMistake(2)
                                            voiceManager.speak("Aim the nozzle at the base of the fire first.")
                                        }
                                    }
                                }
                            }

                            // --- STEP 3: SQUEEZE THE LEVER ---
                            if (currentState == TrainingState.SQUEEZE_LEVER) {
                                if (isSqueezing) {
                                    mainHandler.post {
                                        ext?.setDischarging(true)
                                        audioManager.playLeverSqueezeSound()
                                        audioManager.playExtinguisherDischarge(5000)
                                        viewModel.updateState(TrainingState.SWEEP_SIDE_TO_SIDE)
                                        voiceManager.speak("Keep squeezing and sweep the nozzle from side to side.")
                                    }
                                }
                            }

                            // --- STEP 4: SWEEP SIDE TO SIDE ---
                            if (currentState == TrainingState.SWEEP_SIDE_TO_SIDE) {
                                ext?.setDischarging(isSqueezing)
                                val horizontalAimAngle = Math.atan2(localFwdX.toDouble(), -localFwdZ.toDouble()).toFloat()
                                val deltaAngle = Math.abs(horizontalAimAngle - lastHorizontalAimAngle)
                                lastHorizontalAimAngle = horizontalAimAngle

                                if (isSqueezing) {
                                    if (isAimed && deltaAngle in 0.003f..0.25f) {
                                        accumulatedSweepAngle += deltaAngle
                                        val prog = (accumulatedSweepAngle / 0.50f).coerceIn(0f, 1f)
                                        mainHandler.post {
                                            viewModel.updateSweepProgress(prog)
                                        }
                                        if (prog >= 1.0f) {
                                            mainHandler.post {
                                                ext?.setDischarging(false)
                                                videoFireNode?.stopFire()
                                                audioManager.stopHazardSound()
                                                audioManager.playSuccessChime()
                                                viewModel.updateState(TrainingState.FIRE_EXTINGUISHED)
                                                voiceManager.speak("Fire extinguished! Excellent job following safety procedures.")
                                                mainHandler.postDelayed({
                                                    viewModel.updateState(TrainingState.TRAINING_COMPLETE)
                                                }, 2500)
                                            }
                                        }
                                    }
                                } else if (now - lastWarningVoiceTimeMs > 4000L) {
                                    lastWarningVoiceTimeMs = now
                                    mainHandler.post {
                                        voiceManager.speak("Keep the lever squeezed while sweeping.")
                                    }
                                }
                            }
                        }
                    }

                    // --- MODULE 2: CHEMICAL HAZARD DISTANCE & ISOLATION LOGIC ---
                    if (selectedModule == TrainingModule.CHEMICAL_HAZARD) {
                        val chemVirtualZ = -14.0f
                        val chemVirtualX = 0.8f
                        val distToChem = Math.hypot(
                            (workerVirtualX - chemVirtualX).toDouble(),
                            (workerVirtualZ - chemVirtualZ).toDouble()
                        ).toFloat()

                        mainHandler.post {
                            viewModel.updateDistanceToObjective(distToChem)
                        }

                        // Safe distance check: Danger zone is 2.0 meters
                        if (distToChem < 2.0f && !dangerZoneWarningTriggered && currentState != TrainingState.HAZARD_CONTROLLED) {
                            dangerZoneWarningTriggered = true
                            mainHandler.post {
                                audioManager.playMistakeBuzzer()
                                viewModel.registerMistake(5)
                                voiceManager.speak("Warning! Too close to chemical hazard. Step back immediately!")
                            }
                        } else if (distToChem >= 2.2f) {
                            dangerZoneWarningTriggered = false
                        }

                        if (currentState == TrainingState.CHEMICAL_HAZARD_DETECTED && distToChem >= 3.2f) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.LEAVE_HAZARD_ZONE)
                            }
                        }

                        val kitVirtualZ = -6.0f
                        val kitVirtualX = -1.5f
                        val distToKit = Math.hypot(
                            (workerVirtualX - kitVirtualX).toDouble(),
                            (workerVirtualZ - kitVirtualZ).toDouble()
                        ).toFloat()

                        if (currentState == TrainingState.LEAVE_HAZARD_ZONE && distToKit <= 2.5f) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.LOCATE_EMERGENCY_EQUIPMENT)
                            }
                        }

                        val stationVirtualZ = -12.5f
                        val stationVirtualX = -1.0f
                        val distToStation = Math.hypot(
                            (workerVirtualX - stationVirtualX).toDouble(),
                            (workerVirtualZ - stationVirtualZ).toDouble()
                        ).toFloat()

                        if ((currentState == TrainingState.LEAVE_HAZARD_ZONE || currentState == TrainingState.LOCATE_EMERGENCY_EQUIPMENT) && (distToStation <= 1.8f || distToKit <= 1.5f)) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.PERFORM_SAFE_RESPONSE)
                            }
                        }
                    }
                }
            }
        )

        // Hand Landmarks Skeleton Debug Overlay (Visual verification for MediaPipe tracking)
        if (selectedModule == TrainingModule.ELECTRICAL_FIRE && handGestureState.isHandPresent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val pts = handGestureState.landmarks
                if (pts.size >= 21) {
                    // Finger and palm joint connections for full 21-landmark hand skeleton
                    val connections = listOf(
                        // Thumb
                        0 to 1, 1 to 2, 2 to 3, 3 to 4,
                        // Index
                        0 to 5, 5 to 6, 6 to 7, 7 to 8,
                        // Middle
                        0 to 9, 9 to 10, 10 to 11, 11 to 12,
                        // Ring
                        0 to 13, 13 to 14, 14 to 15, 15 to 16,
                        // Pinky
                        0 to 17, 17 to 18, 18 to 19, 19 to 20,
                        // Palm knuckles
                        5 to 9, 9 to 13, 13 to 17
                    )
                    for ((start, end) in connections) {
                        drawLine(
                            color = Color(0xCC00E676),
                            start = Offset(pts[start].x * canvasW, pts[start].y * canvasH),
                            end = Offset(pts[end].x * canvasW, pts[end].y * canvasH),
                            strokeWidth = 5f
                        )
                    }
                    // Highlight pinch line between Thumb Tip (#4) and Index Tip (#8)
                    if (handGestureState.isPinching) {
                        drawLine(
                            color = Color(0xFFFF1744),
                            start = Offset(pts[4].x * canvasW, pts[4].y * canvasH),
                            end = Offset(pts[8].x * canvasW, pts[8].y * canvasH),
                            strokeWidth = 7f
                        )
                    }
                    // Draw landmark nodes
                    for (i in pts.indices) {
                        val pt = pts[i]
                        val (nodeColor, nodeRadius) = when (i) {
                            4, 8 -> if (handGestureState.isPinching) Color(0xFFFF1744) to 12f else Color(0xFFFFD600) to 10f
                            0 -> Color(0xFF2979FF) to 9f
                            else -> Color(0xFF00E676) to 6f
                        }
                        drawCircle(
                            color = nodeColor,
                            radius = nodeRadius,
                            center = Offset(pt.x * canvasW, pt.y * canvasH)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // COMPLETE INDUSTRIAL AR HUD OVERLAY
        // =========================================================================
        Box(modifier = Modifier.fillMaxSize()) {

            // --- 1. TOP STATUS BAR (Score / Time & Title) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left Glassmorphic Badge: "Score: 100/100 | Time: 0:00"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Score: $score/100",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "  |  ",
                            color = Color.LightGray.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Time: ${timeSeconds / 60}:${String.format(Locale.US, "%02d", timeSeconds % 60)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top-Right Industrial Branding: "MINE SAFE AR / COAL MINE TRAINING"
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.hud_title),
                        color = Color(0xFF00E676),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing * 1.2f
                    )
                    Text(
                        text = stringResource(R.string.hud_subtitle),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- AR & GESTURE STATUS PILLS ---
            val debugBgColor = when {
                doorwayAnchor != null -> Color(0xFF2E7D32)
                debugPlacementStatus.startsWith("FLOOR DETECTED") -> Color(0xFF00C853)
                trackingLost -> Color(0xFFD32F2F)
                else -> Color(0xFF0288D1)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = debugBgColor.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "● $debugPlacementStatus",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val (badgeBg, badgeBorder, badgeText) = when {
                        handGestureState.isLeverSqueezeActive() -> Triple(
                            Color(0xFFD50000).copy(alpha = 0.92f),
                            Color(0xFFFF8A80),
                            "✊ SQUEEZE DETECTED"
                        )
                        handGestureState.isPinching -> Triple(
                            Color(0xFFFF6D00).copy(alpha = 0.92f),
                            Color(0xFFFFD54F),
                            "🤏 PINCH DETECTED"
                        )
                        handGestureState.isHandPresent -> Triple(
                            Color(0xFF00897B).copy(alpha = 0.90f),
                            Color(0xFF80CBC4),
                            "🖐️ HAND DETECTED"
                        )
                        else -> Triple(
                            Color(0xFF37474F).copy(alpha = 0.75f),
                            Color(0xFF78909C).copy(alpha = 0.40f),
                            "○ NO HAND"
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeBorder)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // PASS Procedure Status Badge
                    val isExtHeld = extinguisherNode?.holdingState == ExtinguisherHoldingState.HELD ||
                            currentState in listOf(
                                TrainingState.EXTINGUISHER_HELD,
                                TrainingState.PULL_SAFETY_PIN,
                                TrainingState.AIM_AT_FIRE_BASE,
                                TrainingState.AIM_AT_FIRE,
                                TrainingState.OPEN_NOZZLE,
                                TrainingState.SQUEEZE_LEVER,
                                TrainingState.SWEEP_SIDE_TO_SIDE,
                                TrainingState.DISCHARGE_EXTINGUISHER
                            )
                    if (isExtHeld) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val (procBg, procText) = when (currentState) {
                            TrainingState.PULL_SAFETY_PIN -> Color(0xFFE65100) to "🔒 PIN: LOCKED"
                            TrainingState.AIM_AT_FIRE_BASE, TrainingState.AIM_AT_FIRE, TrainingState.OPEN_NOZZLE ->
                                if (isAimAlignedWithFireBase) Color(0xFF2E7D32) to "🎯 AIM: ALIGNED" else Color(0xFFC62828) to "🎯 AIM: POINT AT BASE"
                            TrainingState.SQUEEZE_LEVER ->
                                if (handGestureState.isLeverSqueezeActive()) Color(0xFF00C853) to "⚡ LEVER: SQUEEZED" else Color(0xFFF57F17) to "⚡ SQUEEZE LEVER"
                            TrainingState.SWEEP_SIDE_TO_SIDE ->
                                Color(0xFF00838F) to "↔ SWEEP: ${(sweepProgress * 100).toInt()}%"
                            else -> Color(0xFF2E7D32) to "🧯 READY"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = procBg.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = procText,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // --- 2. INSTRUCTION CARD ---
            val instructionHeader = when (currentState) {
                TrainingState.START, TrainingState.SCAN_FLOOR, TrainingState.PLACE_DOORWAY -> "Scan the floor to place the mine entrance."
                TrainingState.ENTER_MINE -> stringResource(R.string.instr_enter_mine)
                TrainingState.FIRE_DETECTED, TrainingState.GO_TO_EXTINGUISHER -> stringResource(R.string.instr_fire_detected)
                TrainingState.EXTINGUISHER_REACHED -> stringResource(R.string.instr_extinguisher_reached)
                TrainingState.EXTINGUISHER_HELD -> stringResource(R.string.instr_extinguisher_held)
                TrainingState.PULL_SAFETY_PIN -> "STEP 1: PULL SAFETY PIN"
                TrainingState.AIM_AT_FIRE_BASE, TrainingState.AIM_AT_FIRE, TrainingState.OPEN_NOZZLE -> "STEP 2: AIM AT BASE OF FIRE"
                TrainingState.SQUEEZE_LEVER -> "STEP 3: SQUEEZE THE LEVER"
                TrainingState.SWEEP_SIDE_TO_SIDE -> "STEP 4: SWEEP SIDE TO SIDE"
                TrainingState.DISCHARGE_EXTINGUISHER -> stringResource(R.string.instr_discharge)
                TrainingState.FIRE_EXTINGUISHED -> stringResource(R.string.instr_fire_extinguished)
                TrainingState.CHEMICAL_HAZARD_DETECTED -> stringResource(R.string.instr_chem_detected)
                TrainingState.MAINTAIN_SAFE_DISTANCE, TrainingState.LEAVE_HAZARD_ZONE -> stringResource(R.string.instr_chem_move_away)
                TrainingState.LOCATE_EMERGENCY_EQUIPMENT -> stringResource(R.string.instr_chem_locate_equipment)
                TrainingState.PERFORM_SAFE_RESPONSE -> stringResource(R.string.instr_chem_follow_procedure)
                TrainingState.HAZARD_CONTROLLED -> stringResource(R.string.instr_chem_controlled)
                TrainingState.TRAINING_COMPLETE -> stringResource(R.string.training_complete)
            }

            val instructionDesc = when (currentState) {
                TrainingState.START, TrainingState.SCAN_FLOOR, TrainingState.PLACE_DOORWAY -> "Point camera at the floor until surface is detected, then tap."
                TrainingState.ENTER_MINE -> if (doorDistance > 0) String.format(Locale.US, "Doorway: %.2f m ahead (Walk forward to enter)", doorDistance) else "Walk forward through doorway"
                TrainingState.FIRE_DETECTED -> "Electrical fire ahead beside railway"
                TrainingState.EXTINGUISHER_REACHED -> "Bring thumb & index finger together (pinch 🤏) to pick up extinguisher"
                TrainingState.EXTINGUISHER_HELD -> "Approach the electrical fire (~4m ahead)"
                TrainingState.PULL_SAFETY_PIN -> "Pinch (🤏) the safety pin ring to remove it"
                TrainingState.AIM_AT_FIRE_BASE, TrainingState.AIM_AT_FIRE, TrainingState.OPEN_NOZZLE -> if (isAimAlignedWithFireBase) "Aim locked! Hold steady." else "Point nozzle down at base of fire flames"
                TrainingState.SQUEEZE_LEVER -> "Squeeze hand (✊) to depress lever and discharge"
                TrainingState.SWEEP_SIDE_TO_SIDE -> "Keep lever squeezed! Sweep nozzle left-to-right across flames (${(sweepProgress * 100).toInt()}%)"
                TrainingState.DISCHARGE_EXTINGUISHER -> "Discharging suppression agent"
                TrainingState.CHEMICAL_HAZARD_DETECTED -> "Stay at least 2.0m away from toxic vapor"
                TrainingState.LEAVE_HAZARD_ZONE -> "Retreat to clear vantage point"
                TrainingState.PERFORM_SAFE_RESPONSE -> "Turn emergency isolation valve on wall"
                else -> ""
            }

            Card(
                modifier = Modifier
                    .padding(top = 76.dp, start = 16.dp)
                    .widthIn(max = 300.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14171A).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⛶", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = instructionHeader,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (instructionDesc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = instructionDesc,
                                color = Color(0xFFB0BEC5),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // --- 3. RIGHT-SIDE VERTICAL ACTION DOCK ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Settings Button
                FilledIconButton(
                    onClick = { showSettingsModal = true },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Text("⚙", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }

                // Help Button
                FilledIconButton(
                    onClick = { showHelpModal = true },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Text("?", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Sound Toggle Button
                FilledIconButton(
                    onClick = {
                        viewModel.toggleSound()
                        val muted = !isSoundMuted
                        voiceManager.isMuted = muted
                        audioManager.isMuted = muted
                        if (muted) audioManager.stopMineAmbiance()
                    },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        contentColor = if (isSoundMuted) Color(0xFFEF5350) else Color.White
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Text(if (isSoundMuted) "🔇" else "🔊", style = MaterialTheme.typography.titleMedium)
                }
            }

            // --- 4. INTERACTIVE ACTIONS HUD (Nozzle, Aim, Discharge, Valve) ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentState == TrainingState.PULL_SAFETY_PIN) {
                    Button(
                        onClick = {
                            extinguisherNode?.triggerPinRemoval()
                            audioManager.playPinPullSound()
                            viewModel.updateState(TrainingState.AIM_AT_FIRE_BASE)
                            voiceManager.speak("Safety pin removed! Now aim the nozzle at the base of the fire.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("PULL SAFETY PIN (🤏)", fontWeight = FontWeight.Bold)
                    }
                }

                if (currentState == TrainingState.AIM_AT_FIRE_BASE) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAimAlignedWithFireBase) Color(0xFF2E7D32).copy(alpha = 0.90f) else Color(0xFF37474F).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (isAimAlignedWithFireBase) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAimAlignedWithFireBase) "🎯 AIM ALIGNED WITH FIRE BASE" else "🎯 POINT NOZZLE AT BASE OF FLAMES",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (currentState == TrainingState.SQUEEZE_LEVER) {
                    Button(
                        onClick = {
                            extinguisherNode?.setLeverSqueezed(true)
                            extinguisherNode?.setDischarging(true)
                            audioManager.playLeverSqueezeSound()
                            audioManager.playExtinguisherDischarge(5000)
                            viewModel.updateState(TrainingState.SWEEP_SIDE_TO_SIDE)
                            voiceManager.speak("Keep squeezing and sweep side to side.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(stringResource(R.string.btn_squeeze_lever), fontWeight = FontWeight.Bold)
                    }
                }

                if (currentState == TrainingState.SWEEP_SIDE_TO_SIDE) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF00E676)),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SWEEPING ACROSS FLAMES: ${(sweepProgress * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { sweepProgress },
                                modifier = Modifier.width(220.dp).height(8.dp),
                                color = Color(0xFF00E676),
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }

                if (currentState == TrainingState.PERFORM_SAFE_RESPONSE) {
                    Button(
                        onClick = {
                            emergencyStationNode?.activateValve()
                            chemicalNode?.setHazardControlled(true)
                            viewModel.updateState(TrainingState.HAZARD_CONTROLLED)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(stringResource(R.string.btn_isolate_valve), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- 5. BOTTOM INSTRUCTION TOAST ---
            if (doorwayAnchor == null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👇", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentFloorHit != null) "Floor detected! Tap to place the mine entrance" else "Scan the floor to place the mine entrance",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tracking lost warning
            if (trackingLost) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.90f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ar_tracking_lost),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (showHelpModal) {
            HelpModal(onDismiss = { showHelpModal = false })
        }

        if (showSettingsModal) {
            SettingsModal(
                isSoundMuted = isSoundMuted,
                onToggleSound = {
                    viewModel.toggleSound()
                    val muted = !isSoundMuted
                    voiceManager.isMuted = muted
                    audioManager.isMuted = muted
                    if (muted) audioManager.stopMineAmbiance()
                },
                currentLanguage = voiceManager.currentLanguageCode,
                onLanguageChange = { lang -> voiceManager.setLanguage(lang) },
                onDismiss = { showSettingsModal = false }
            )
        }
    }
}
