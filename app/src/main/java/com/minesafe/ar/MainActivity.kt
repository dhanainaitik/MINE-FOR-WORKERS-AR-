package com.minesafe.ar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minesafe.ar.audio.AudioManager
import com.minesafe.ar.audio.VoiceInstructionManager
import com.minesafe.ar.training.TrainingState
import com.minesafe.ar.training.TrainingViewModel
import com.minesafe.ar.ui.AppScreen
import com.minesafe.ar.ui.MainApp
import com.minesafe.ar.ui.ResultsScreen
import com.minesafe.ar.ui.StartScreen
import com.minesafe.ar.ui.theme.MineSafeARTheme

class MainActivity : ComponentActivity() {
    private var voiceManager: VoiceInstructionManager? = null
    private var audioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vManager = VoiceInstructionManager(this)
        val aManager = AudioManager(this)
        voiceManager = vManager
        audioManager = aManager

        setContent {
            MineSafeARTheme {
                val context = LocalContext.current
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                }

                LaunchedEffect(Unit) {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                val viewModel: TrainingViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(AppScreen.START) }

                val selectedModule by viewModel.selectedModule.collectAsState()
                val score by viewModel.score.collectAsState()
                val timeSeconds by viewModel.timeSeconds.collectAsState()
                val mistakes by viewModel.mistakes.collectAsState()
                val stepsCompleted by viewModel.stepsCompleted.collectAsState()
                val isSoundMuted by viewModel.isSoundMuted.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        when (currentScreen) {
                            AppScreen.START -> {
                                StartScreen(
                                    selectedModule = selectedModule,
                                    onSelectModule = { module -> viewModel.selectModule(module) },
                                    onStartTraining = {
                                        if (!hasCameraPermission) {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                        viewModel.resetTraining()
                                        viewModel.updateState(TrainingState.PLACE_DOORWAY)
                                        currentScreen = AppScreen.TRAINING
                                    },
                                    isSoundMuted = isSoundMuted,
                                    onToggleSound = {
                                        viewModel.toggleSound()
                                        val muted = !isSoundMuted
                                        vManager.isMuted = muted
                                        aManager.isMuted = muted
                                        if (muted) aManager.stopMineAmbiance()
                                    },
                                    currentLanguage = vManager.currentLanguageCode,
                                    onLanguageSelected = { lang ->
                                        vManager.setLanguage(lang)
                                    }
                                )
                            }
                            AppScreen.TRAINING -> {
                                if (hasCameraPermission) {
                                    MainApp(
                                        viewModel = viewModel,
                                        voiceManager = vManager,
                                        audioManager = aManager,
                                        onTrainingComplete = { currentScreen = AppScreen.RESULTS }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Camera Permission Required",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "AR floor scanning requires camera access to detect the mine floor.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                                Text("Grant Camera Permission")
                                            }
                                        }
                                    }
                                }
                            }
                            AppScreen.RESULTS -> {
                                ResultsScreen(
                                    module = selectedModule,
                                    score = score,
                                    timeSeconds = timeSeconds,
                                    mistakes = mistakes,
                                    stepsCompleted = stepsCompleted,
                                    totalSteps = viewModel.totalSteps,
                                    onRestart = {
                                        viewModel.resetTraining()
                                        viewModel.updateState(TrainingState.PLACE_DOORWAY)
                                        currentScreen = AppScreen.TRAINING
                                    },
                                    onHome = {
                                        currentScreen = AppScreen.START
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.shutdown()
        audioManager?.shutdown()
    }
}