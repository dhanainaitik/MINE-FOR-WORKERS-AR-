package com.minesafe.ar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minesafe.ar.R
import com.minesafe.ar.training.TrainingModule

@Composable
fun StartScreen(
    selectedModule: TrainingModule,
    onSelectModule: (TrainingModule) -> Unit,
    onStartTraining: () -> Unit,
    isSoundMuted: Boolean,
    onToggleSound: () -> Unit,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var showHelpModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF14171A),
            Color(0xFF1E2328),
            Color(0xFF0F1114)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // HEADER & BRANDING
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Industrial Hazard Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFB300),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "  INDUSTRIAL AR TRAINING PLATFORM  ",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = stringResource(id = R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Black,
                    letterSpacing = MaterialTheme.typography.displayMedium.letterSpacing * 1.5f
                )

                Text(
                    text = stringResource(id = R.string.app_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB0BEC5),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // MODULE SELECTION CARDS
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.select_module),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )

                // MODULE 01: ELECTRICAL FIRE SAFETY
                val isMod1Selected = selectedModule == TrainingModule.ELECTRICAL_FIRE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModule(TrainingModule.ELECTRICAL_FIRE) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isMod1Selected) 2.dp else 1.dp,
                        color = if (isMod1Selected) Color(0xFFFF5722) else Color(0xFF374151)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMod1Selected) Color(0xFF2A1C18) else Color(0xFF1E2226)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.module_1_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isMod1Selected) Color(0xFFFF7043) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (isMod1Selected) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFF5722)
                                ) {
                                    Text(
                                        text = " ACTIVE ",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(id = R.string.module_1_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }

                // MODULE 02: CHEMICAL HAZARD RESPONSE
                val isMod2Selected = selectedModule == TrainingModule.CHEMICAL_HAZARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModule(TrainingModule.CHEMICAL_HAZARD) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isMod2Selected) 2.dp else 1.dp,
                        color = if (isMod2Selected) Color(0xFF64DD17) else Color(0xFF374151)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMod2Selected) Color(0xFF1A2616) else Color(0xFF1E2226)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.module_2_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isMod2Selected) Color(0xFF76FF03) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (isMod2Selected) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text(
                                        text = " ACTIVE ",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(id = R.string.module_2_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // ACTION BUTTONS: START TRAINING & DOCK
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PRIMARY START BUTTON
                Button(
                    onClick = onStartTraining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.start_training),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing * 1.3f
                    )
                }

                // BOTTOM UTILITY BUTTON ROW: SETTINGS, HELP, SOUND, LANGUAGE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSettingsModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF4B5563))
                    ) {
                        Text(stringResource(id = R.string.settings), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showHelpModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF4B5563))
                    ) {
                        Text(stringResource(id = R.string.help), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onToggleSound,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSoundMuted) Color(0xFFEF5350) else Color(0xFF4B5563))
                    ) {
                        Text(
                            text = if (isSoundMuted) "MUTED" else "SOUND",
                            color = if (isSoundMuted) Color(0xFFEF5350) else Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val nextLang = when (currentLanguage) {
                                "en" -> "hi"
                                "hi" -> "sat"
                                else -> "en"
                            }
                            onLanguageSelected(nextLang)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Text(
                            text = when (currentLanguage) {
                                "en" -> "HI"
                                "hi" -> "SAT"
                                else -> "EN"
                            },
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        if (showHelpModal) {
            HelpModal(onDismiss = { showHelpModal = false })
        }

        if (showSettingsModal) {
            SettingsModal(
                isSoundMuted = isSoundMuted,
                onToggleSound = onToggleSound,
                currentLanguage = currentLanguage,
                onLanguageChange = { lang -> onLanguageSelected(lang) },
                onDismiss = { showSettingsModal = false }
            )
        }
    }
}
