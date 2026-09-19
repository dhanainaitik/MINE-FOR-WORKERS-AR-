package com.minesafe.ar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.minesafe.ar.R

@Composable
fun HelpModal(
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
                    text = stringResource(R.string.help_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.help_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
                )

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
