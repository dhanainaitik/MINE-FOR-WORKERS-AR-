package com.minesafe.ar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.minesafe.ar.R

@Composable
fun SettingsModal(
    isSoundMuted: Boolean,
    onToggleSound: () -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sound Effects & Voice Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sound),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = !isSoundMuted,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD54F),
                            checkedTrackColor = Color(0xFF424242)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection
                Text(
                    text = stringResource(R.string.language),
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onLanguageChange("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == "en") Color(0xFFFFD54F) else Color(0xFF374151),
                            contentColor = if (currentLanguage == "en") Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.english), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onLanguageChange("hi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLanguage == "hi") Color(0xFFFFD54F) else Color(0xFF374151),
                            contentColor = if (currentLanguage == "hi") Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hindi), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
