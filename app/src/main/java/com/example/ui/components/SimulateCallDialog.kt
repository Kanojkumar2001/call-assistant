package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HighUrgencyRed
import com.example.ui.theme.IndigoPrimary

@Composable
fun SimulateCallDialog(
    onDismiss: () -> Unit,
    onSimulate: (callerName: String, phone: String, category: String, text: String) -> Unit
) {
    var selectedPreset by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var customPhone by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Emergency") }
    var customText by remember { mutableStateOf("") }

    val presets = remember {
        listOf(
            PresetCall(
                title = "Emergency Hospital Call",
                callerName = "Dr. Eleanor Harrison",
                phone = "+1 (555) 911-0421",
                category = "Doctor",
                transcript = "This is Dr. Harrison from St. Mary's ER calling with an urgent medical update regarding your family member's lab tests. Please call back our direct line immediately upon listening."
            ),
            PresetCall(
                title = "High Stakes Business Contract",
                callerName = "Alex Carter (CEO)",
                phone = "+1 (555) 883-2049",
                category = "Business",
                transcript = "Alex here. Board meeting was moved up to 3 PM today. We need the final signed architectural contract and liability waiver sent over right away."
            ),
            PresetCall(
                title = "Express Delivery Driver",
                callerName = "Apex Driver - Unit 402",
                phone = "+1 (800) 555-0199",
                category = "General",
                transcript = "Hi, I'm at the front gate with your perishable delivery. I need the gate pin code or someone to sign at the main entrance."
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simulate Incoming AI Voicemail",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select a call persona or type custom text to test Gemini Speech-to-Text & Urgency Analysis:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                presets.forEachIndexed { index, preset ->
                    FilterChip(
                        selected = selectedPreset == index,
                        onClick = { selectedPreset = index },
                        label = {
                            Text("${preset.title} (${preset.category})", style = MaterialTheme.typography.labelMedium)
                        },
                        leadingIcon = {
                            val icon = when (preset.category) {
                                "Doctor" -> Icons.Default.LocalHospital
                                "Business" -> Icons.Default.Business
                                else -> Icons.Default.LocalPostOffice
                            }
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }

                FilterChip(
                    selected = selectedPreset == 3,
                    onClick = { selectedPreset = 3 },
                    label = { Text("✏️ Custom Voice Message", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )

                if (selectedPreset == 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Caller Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customPhone,
                        onValueChange = { customPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Spoken Voice Message Text") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${presets[selectedPreset].transcript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPreset < 3) {
                        val p = presets[selectedPreset]
                        onSimulate(p.callerName, p.phone, p.category, p.transcript)
                    } else {
                        val name = if (customName.isNotBlank()) customName else "Unknown Caller"
                        val phone = if (customPhone.isNotBlank()) customPhone else "+1 (555) 000-1122"
                        val text = if (customText.isNotBlank()) customText else "Hello, please call me back as soon as possible."
                        onSimulate(name, phone, customCategory, text)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Process AI Voicemail")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class PresetCall(
    val title: String,
    val callerName: String,
    val phone: String,
    val category: String,
    val transcript: String
)
