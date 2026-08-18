package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.VoicemailEntity
import com.example.data.repository.AuthState
import com.example.ui.MainViewModel
import com.example.ui.components.CallVolumeBarChart
import com.example.ui.components.SimulateCallDialog
import com.example.ui.components.UrgencyDistributionChart
import com.example.ui.components.VoicemailCard
import com.example.ui.components.VoicemailDetailSheet
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToVoicemails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()
    val analytics by viewModel.analyticsSummary.collectAsState()
    val voicemails by viewModel.allVoicemails.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedVoicemail by viewModel.selectedVoicemail.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()

    var showSimulateDialog by remember { mutableStateOf(false) }

    val user = (authState as? AuthState.Authenticated)?.user

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hello, ${user?.displayName ?: "User"} 👋",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Assistant is ${settings?.assistantMode ?: "ACTIVE"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LowUrgencyGreen
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LowUrgencyGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSimulateDialog = true },
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null) },
                text = { Text("Simulate AI Call") },
                containerColor = IndigoPrimary,
                contentColor = Color.White
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // KPI Metrics Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Missed Calls",
                        value = "${analytics.totalMissedCalls}",
                        subtitle = "Today",
                        icon = Icons.Default.Call,
                        accentColor = BlueAccent,
                        modifier = Modifier.weight(1f)
                    )

                    KpiMetricCard(
                        title = "Voicemails",
                        value = "${analytics.totalVoicemails}",
                        subtitle = "Recorded",
                        icon = Icons.Default.RecordVoiceOver,
                        accentColor = IndigoPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // KPI Metrics Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Urgent Alerts",
                        value = "${analytics.highUrgencyCount}",
                        subtitle = "Action Needed",
                        icon = Icons.Default.Warning,
                        accentColor = HighUrgencyRed,
                        modifier = Modifier.weight(1f)
                    )

                    KpiMetricCard(
                        title = "Avg Duration",
                        value = "${analytics.avgDurationSeconds}s",
                        subtitle = "Per Message",
                        icon = Icons.Default.Timer,
                        accentColor = MediumUrgencyAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Processing Loader if simulating
            item {
                AnimatedVisibility(visible = isSimulating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Simulating Incoming Call & Processing AI Transcript...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Running Speech-to-Text & Gemini Urgency Analysis",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Real-time Visual Charts
            item {
                CallVolumeBarChart(dailyVolume = analytics.dailyCallVolume)
            }

            item {
                UrgencyDistributionChart(urgencyDistribution = analytics.urgencyDistribution)
            }

            // Recent AI Voicemails Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent AI Voicemail Feed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    TextButton(onClick = onNavigateToVoicemails) {
                        Text("View All (${voicemails.size})")
                    }
                }
            }

            // Voicemail List items (top 3)
            items(voicemails.take(3), key = { it.id }) { vm ->
                VoicemailCard(
                    voicemail = vm,
                    onClick = { viewModel.selectVoicemailForDetail(vm) },
                    onDelete = { viewModel.deleteVoicemail(vm.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showSimulateDialog) {
        SimulateCallDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { name, phone, cat, text ->
                showSimulateDialog = false
                viewModel.simulateCall(name, phone, cat, text)
            }
        )
    }

    selectedVoicemail?.let { vm ->
        VoicemailDetailSheet(
            voicemail = vm,
            onDismiss = { viewModel.selectVoicemailForDetail(null) }
        )
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
