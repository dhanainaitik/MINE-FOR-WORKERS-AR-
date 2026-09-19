package com.minesafe.ar.ui

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.minesafe.ar.R
import com.minesafe.ar.ar.ChemicalHazardNode
import com.minesafe.ar.ar.DoorwayNode
import com.minesafe.ar.ar.EmergencyStationNode
import com.minesafe.ar.ar.ExtinguisherNode
import com.minesafe.ar.ar.FireNode
import com.minesafe.ar.ar.MineEnvironmentNode
import com.minesafe.ar.ar.PlacementReticleNode
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
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
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
    var fireNode by remember { mutableStateOf<FireNode?>(null) }
    var extinguisherNode by remember { mutableStateOf<ExtinguisherNode?>(null) }
    var chemicalNode by remember { mutableStateOf<ChemicalHazardNode?>(null) }
    var emergencyStationNode by remember { mutableStateOf<EmergencyStationNode?>(null) }

    var isAimingAtFire by remember { mutableStateOf(false) }
    var dangerZoneWarningTriggered by remember { mutableStateOf(false) }

    // Directional illumination down the mine drift
    val mainLight = rememberMainLightNode(engine) {
        color = Float4(1.0f, 0.95f, 0.85f, 1.0f)
        intensity = 160000f
        lightDirection = Float3(0.15f, -0.75f, -0.65f)
    }

    // Periodic nozzle reminder reaction
    LaunchedEffect(nozzleReminderCount) {
        if (nozzleReminderCount > 0 && currentState == TrainingState.EXTINGUISHER_REACHED) {
            voiceManager.speak("Please open the extinguisher nozzle.")
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
            // Module 1: Electrical Fire
            TrainingState.FIRE_DETECTED -> {
                audioManager.startMineAmbiance()
                audioManager.startFireSound()
                voiceManager.speak("Warning. Electrical fire detected. Move toward the fire extinguisher.")
            }
            TrainingState.EXTINGUISHER_REACHED -> {
                audioManager.playSuccessChime()
                voiceManager.speak("Good. Pick up the extinguisher and prepare to operate it.")
            }
            TrainingState.OPEN_NOZZLE -> {
                audioManager.playTap()
                voiceManager.speak("Aim the extinguisher toward the base of the fire.")
            }
            TrainingState.AIM_AT_FIRE -> {
                voiceManager.speak("Discharge the extinguisher.")
            }
            TrainingState.DISCHARGE_EXTINGUISHER -> {
                audioManager.playExtinguisherDischarge()
            }
            TrainingState.FIRE_EXTINGUISHED -> {
                audioManager.stopHazardSound()
                audioManager.playSuccessChime()
                voiceManager.speak("Training complete. The fire has been extinguished.")
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

                                    // 4. Training Hazards & Equipment inside the virtual mine container
                                    if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                                        // Electrical Fire Cabinet (virtual Z = -11.0m, X = +0.8m)
                                        val fNode = FireNode(
                                            engine = engine,
                                            cabinetMaterial = darkMetalMat,
                                            fireCoreMaterial = fireCoreMat,
                                            fireOuterMaterial = fireOuterMat,
                                            smokeMaterial = smokeMat,
                                            warningSignMaterial = hazardYellowMat
                                        ).apply {
                                            position = Float3(0.8f, 0.0f, -11.0f)
                                        }
                                        fireNode = fNode
                                        mineContainer.addChildNode(fNode)

                                        // Fire Extinguisher (virtual Z = -8.0m, X = +1.1m)
                                        val extNode = ExtinguisherNode(
                                            engine = engine,
                                            redBodyMaterial = extRedMat,
                                            metalMaterial = darkMetalMat,
                                            brassMaterial = brassMat,
                                            rubberMaterial = rubberMat,
                                            sprayMaterial = sprayMat,
                                            gaugeGreenMaterial = greenGaugeMat
                                        ).apply {
                                            position = Float3(1.1f, 0.0f, -8.0f)
                                        }
                                        extinguisherNode = extNode
                                        mineContainer.addChildNode(extNode)
                                    } else {
                                        // Module 2: Chemical Hazard
                                        val cNode = ChemicalHazardNode(
                                            engine = engine,
                                            drumYellowMaterial = hazardYellowMat,
                                            hazardBandMaterial = hazardBandMat,
                                            vaporMaterial = chemVaporMat,
                                            puddleMaterial = chemPuddleMat,
                                            pipeMaterial = pipeMat,
                                            perimeterMaterial = hazardYellowMat
                                        ).apply {
                                            position = Float3(0.8f, 0.0f, -14.0f)
                                        }
                                        chemicalNode = cNode
                                        mineContainer.addChildNode(cNode)

                                        val eNode = EmergencyStationNode(
                                            engine = engine,
                                            boardMaterial = greenStationMat,
                                            cabinetMaterial = hazardYellowMat,
                                            valveMaterial = extRedMat,
                                            metalMaterial = darkMetalMat,
                                            whiteCrossMaterial = whiteMat
                                        ).apply {
                                            position = Float3(-1.0f, 0.0f, -12.5f)
                                        }
                                        emergencyStationNode = eNode
                                        mineContainer.addChildNode(eNode)
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

                    // Interactive touch actions on 3D equipment
                    when (currentState) {
                        TrainingState.EXTINGUISHER_REACHED -> {
                            if (hitResult?.node == extinguisherNode?.safetyPin || hitResult?.node == extinguisherNode?.nozzle) {
                                extinguisherNode?.removeSafetyPin()
                                extinguisherNode?.openNozzle()
                                viewModel.updateState(TrainingState.OPEN_NOZZLE)
                                viewModel.updateState(TrainingState.AIM_AT_FIRE)
                            }
                        }
                        TrainingState.PERFORM_SAFE_RESPONSE -> {
                            if (hitResult?.node == emergencyStationNode?.isolationValve || hitResult?.node == emergencyStationNode?.isolationLever) {
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

                    // --- MODULE 1: PROXIMITY & AIMING LOGIC ---
                    if (selectedModule == TrainingModule.ELECTRICAL_FIRE) {
                        // Extinguisher is at virtual (1.1, 0.0, -8.0)
                        val extVirtualZ = -8.0f
                        val extVirtualX = 1.1f
                        val distToExt = Math.hypot(
                            (workerVirtualX - extVirtualX).toDouble(),
                            (workerVirtualZ - extVirtualZ).toDouble()
                        ).toFloat()

                        mainHandler.post {
                            viewModel.updateDistanceToObjective(distToExt)
                        }

                        if ((currentState == TrainingState.FIRE_DETECTED || currentState == TrainingState.GO_TO_EXTINGUISHER) && distToExt <= 1.8f) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.EXTINGUISHER_REACHED)
                            }
                        }

                        // Check Aiming vector toward the base of the fire
                        val fPos = fireNode?.worldPosition
                        if (fPos != null && (currentState == TrainingState.AIM_AT_FIRE || currentState == TrainingState.OPEN_NOZZLE)) {
                            val camPos = Float3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
                            val camForward = Float3(cameraPose.zAxis[0], cameraPose.zAxis[1], cameraPose.zAxis[2]) * -1.0f
                            val toFire = normalize(fPos - camPos)
                            val aimCos = dot(camForward, toFire)

                            mainHandler.post {
                                isAimingAtFire = aimCos > 0.92f // within ~23 degrees of fire base
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

                        val stationVirtualZ = -12.5f
                        val stationVirtualX = -1.0f
                        val distToStation = Math.hypot(
                            (workerVirtualX - stationVirtualX).toDouble(),
                            (workerVirtualZ - stationVirtualZ).toDouble()
                        ).toFloat()

                        if ((currentState == TrainingState.LEAVE_HAZARD_ZONE || currentState == TrainingState.LOCATE_EMERGENCY_EQUIPMENT) && distToStation <= 1.8f) {
                            mainHandler.post {
                                viewModel.updateState(TrainingState.PERFORM_SAFE_RESPONSE)
                            }
                        }
                    }
                }
            }
        )

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

            // --- AR STATUS PILL ---
            val debugBgColor = when {
                doorwayAnchor != null -> Color(0xFF2E7D32)
                debugPlacementStatus.startsWith("FLOOR DETECTED") -> Color(0xFF00C853)
                trackingLost -> Color(0xFFD32F2F)
                else -> Color(0xFF0288D1)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = debugBgColor.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Text(
                    text = "● $debugPlacementStatus",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // --- 2. INSTRUCTION CARD ---
            val instructionHeader = when (currentState) {
                TrainingState.START, TrainingState.SCAN_FLOOR, TrainingState.PLACE_DOORWAY -> "Scan the floor to place the mine entrance."
                TrainingState.ENTER_MINE -> stringResource(R.string.instr_enter_mine)
                TrainingState.FIRE_DETECTED, TrainingState.GO_TO_EXTINGUISHER -> stringResource(R.string.instr_fire_detected)
                TrainingState.EXTINGUISHER_REACHED -> stringResource(R.string.instr_extinguisher_reached)
                TrainingState.OPEN_NOZZLE, TrainingState.AIM_AT_FIRE -> stringResource(R.string.instr_aim_at_fire)
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
                TrainingState.FIRE_DETECTED -> "Target extinguisher ~8m virtual (Walk ~0.8m forward)"
                TrainingState.EXTINGUISHER_REACHED -> "Tap nozzle or pull safety pin"
                TrainingState.OPEN_NOZZLE, TrainingState.AIM_AT_FIRE -> if (isAimingAtFire) "Aimed at base of fire. Ready to discharge." else "Aim camera directly at base of fire"
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
                if (currentState == TrainingState.EXTINGUISHER_REACHED) {
                    Button(
                        onClick = {
                            extinguisherNode?.removeSafetyPin()
                            extinguisherNode?.openNozzle()
                            viewModel.updateState(TrainingState.OPEN_NOZZLE)
                            viewModel.updateState(TrainingState.AIM_AT_FIRE)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(stringResource(R.string.btn_open_nozzle), fontWeight = FontWeight.Bold)
                    }
                }

                if (currentState == TrainingState.AIM_AT_FIRE || currentState == TrainingState.OPEN_NOZZLE) {
                    Button(
                        onClick = {
                            if (isAimingAtFire) {
                                viewModel.updateState(TrainingState.DISCHARGE_EXTINGUISHER)
                                extinguisherNode?.setDischarging(true)
                                fireNode?.setFireScale(0.0f)
                                viewModel.updateState(TrainingState.FIRE_EXTINGUISHED)
                            } else {
                                viewModel.registerMistake(5)
                                audioManager.playMistakeBuzzer()
                                voiceManager.speak("Aim directly at the base of the fire before discharging.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAimingAtFire) Color(0xFFFF3D00) else Color(0xFF455A64),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(stringResource(R.string.btn_discharge), fontWeight = FontWeight.Bold)
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
