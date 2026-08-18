package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.VoicemailEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicemailDetailSheet(
    voicemail: VoicemailEntity,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSmsDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Caller info & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (voicemail.urgencyLevel == "HIGH") HighUrgencyRed.copy(alpha = 0.2f)
                                else IndigoPrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = voicemail.callerName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (voicemail.urgencyLevel == "HIGH") HighUrgencyRed else IndigoPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = voicemail.callerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = voicemail.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Player Waveform
            AudioWaveformPlayer(
                durationSeconds = voicemail.durationSeconds,
                barCount = 32
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Urgency & Sentiment Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val urgencyColor = when (voicemail.urgencyLevel) {
                    "HIGH" -> HighUrgencyRed
                    "MEDIUM" -> MediumUrgencyAmber
                    else -> LowUrgencyGreen
                }

                AssistChip(
                    onClick = {},
                    label = { Text("Priority: ${voicemail.urgencyLevel}") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = urgencyColor)
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = urgencyColor.copy(alpha = 0.15f))
                )

                AssistChip(
                    onClick = {},
                    label = { Text("Category: ${voicemail.category}") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                AssistChip(
                    onClick = {},
                    label = { Text(voicemail.sentiment) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Summary Box
            Text(
                text = "AI Voice Analysis & Summary",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Summary:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = voicemail.aiSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Suggested Action: ${voicemail.actionSuggested}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = HighUrgencyRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full Speech-to-Text Transcript
            Text(
                text = "Full Speech-to-Text Transcript",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "\"${voicemail.transcript}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${voicemail.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Back")
                }

                OutlinedButton(
                    onClick = { showSmsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Reply")
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Voicemail from ${voicemail.callerName}:\n\nSummary: ${voicemail.aiSummary}\n\nTranscript: ${voicemail.transcript}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Voicemail Summary"))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSmsDialog) {
        val suggestedSms = "Hi ${voicemail.callerName}, I received your voicemail regarding ${voicemail.category}. I have reviewed the AI transcript and will follow up with you shortly!"
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("Send AI Suggested Quick Reply") },
            text = {
                Column {
                    Text("Suggested SMS Response:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = suggestedSms,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSmsDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${voicemail.phoneNumber}")).apply {
                            putExtra("sms_body", suggestedSms)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Messages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
