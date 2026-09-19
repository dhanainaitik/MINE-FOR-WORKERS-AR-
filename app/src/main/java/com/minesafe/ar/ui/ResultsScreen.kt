package com.minesafe.ar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minesafe.ar.R
import com.minesafe.ar.training.TrainingModule
import java.util.Locale

@Composable
fun ResultsScreen(
    module: TrainingModule,
    score: Int,
    timeSeconds: Int,
    mistakes: Int,
    stepsCompleted: Int,
    totalSteps: Int,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    val minutes = timeSeconds / 60
    val seconds = timeSeconds % 60
    val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val isPassed = score >= 75

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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFB300),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "  VOCATIONAL ASSESSMENT REPORT  ",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = stringResource(id = R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (module == TrainingModule.ELECTRICAL_FIRE) {
                        stringResource(id = R.string.module_1_title)
                    } else {
                        stringResource(id = R.string.module_2_title)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0BEC5)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scorecard
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828)
                ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2226))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.status_label),
                            color = Color(0xFFB0BEC5),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPassed) Color(0xFF2E7D32) else Color(0xFFC62828)
                        ) {
                            Text(
                                text = if (isPassed) "  TRAINING PASSED  " else "  REVIEW REQUIRED  ",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF374151), thickness = 1.dp)

                    // Safety Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.safety_score),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$score / 100",
                            color = if (isPassed) Color(0xFF66BB6A) else Color(0xFFEF5350),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Response Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.response_time),
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = timeString,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Mistakes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.mistakes),
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = "$mistakes",
                            color = if (mistakes == 0) Color(0xFF66BB6A) else Color(0xFFFFB74D),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Steps Completed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.steps_completed),
                            color = Color(0xFFB0BEC5)
                        )
                        Text(
                            text = "$stepsCompleted / $totalSteps",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.restart_training),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF4B5563))
                ) {
                    Text(
                        text = stringResource(id = R.string.back_to_home),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
