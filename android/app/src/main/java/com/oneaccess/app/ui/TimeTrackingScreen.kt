package com.oneaccess.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneaccess.app.net.ApiClient
import com.oneaccess.app.net.TimeSessionResponse
import com.oneaccess.app.net.TimeSummaryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimeTrackingScreen(
    backendUrl: String,
    accessToken: String?,
    onEvent: (String) -> Unit,
    context: Context,
    scope: CoroutineScope
) {
    var currentSession by remember { mutableStateOf<TimeSessionResponse?>(null) }
    var summary by remember { mutableStateOf<TimeSummaryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Auto-refresh current session every 5 seconds if there's an active session
    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            while (true) {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val api = ApiClient(backendUrl.trim().trimEnd('/'))
                        val session = api.getCurrentTimeSession(accessToken)
                        val summaryData = api.getTimeSummary(accessToken)
                        withContext(Dispatchers.Main) {
                            currentSession = session
                            summary = summaryData
                            loading = false
                            error = null
                        }
                    }.onFailure { t ->
                        withContext(Dispatchers.Main) {
                            error = t.message ?: t.toString()
                            loading = false
                        }
                    }
                }
                delay(5000) // Refresh every 5 seconds
            }
        } else {
            loading = false
            error = "Please sign in to view time tracking"
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Time Tracking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (loading) {
            Text("Loading...", style = MaterialTheme.typography.bodyMedium)
        } else if (error != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error ?: "Unknown error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            // Current Session Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentSession != null) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Current Session",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (currentSession != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status:", style = MaterialTheme.typography.bodyMedium)
                            Text("INSIDE BUILDING", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Gate:", style = MaterialTheme.typography.bodyMedium)
                            Text(currentSession!!.gateIdEntry, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(currentSession!!.entryTime.substring(11, 19), fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Time Inside:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                currentSession!!.currentDurationFormatted,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            "No active session - You are currently outside",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Card
            if (summary != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Statistics",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("Today", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sessions:", style = MaterialTheme.typography.bodyMedium)
                            Text("${summary!!.todaySessions}", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(summary!!.todayTimeFormatted, fontWeight = FontWeight.Medium)
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("All Time", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Sessions:", style = MaterialTheme.typography.bodyMedium)
                            Text("${summary!!.totalSessions}", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(summary!!.totalTimeFormatted, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(summary!!.averageTimeFormatted, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        "ℹ️ How it works",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Time tracking starts when you tap IN at a building gate and ends when you tap OUT. " +
                        "This screen auto-updates every 5 seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
