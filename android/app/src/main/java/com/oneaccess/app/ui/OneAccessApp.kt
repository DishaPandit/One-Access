package com.oneaccess.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oneaccess.app.net.ApiClient
import com.oneaccess.app.net.TimeSessionResponse
import com.oneaccess.app.net.TimeSummaryResponse
import com.oneaccess.app.qr.QRCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneAccessApp() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var backendUrl by remember { mutableStateOf(AppState.backendUrl(context)) }
    var email by remember { mutableStateOf(AppState.email(context)) }
    var gateId by remember { mutableStateOf(AppState.gateId(context)) }
    var accessToken by remember { mutableStateOf(AppState.accessToken(context)) }
    val events = remember { mutableStateListOf<String>() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Access", "Delegate", "Visitors", "Time")

    LaunchedEffect(backendUrl) { AppState.setBackendUrl(context, backendUrl) }
    LaunchedEffect(email) { AppState.setEmail(context, email) }
    LaunchedEffect(gateId) { AppState.setGateId(context, gateId) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("OneAccess", fontWeight = FontWeight.SemiBold) },
                actions = {
                    StatusPill(
                        text = when {
                            accessToken == null -> "Not signed in"
                            else -> "Ready"
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                scrollBehavior = scrollBehavior
            )

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        AccessCard(
                            gateId = gateId,
                            signedIn = accessToken != null,
                            accessToken = accessToken,
                            backendUrl = backendUrl,
                            onEvent = { event -> events.add(0, event) }
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Sign In", fontWeight = FontWeight.SemiBold)

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = gateId,
                                    onValueChange = { gateId = it },
                                    label = { Text("Gate ID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                var showAdvanced by remember { mutableStateOf(false) }
                                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                                    Text(if (showAdvanced) "Hide Advanced" else "Advanced")
                                }
                                if (showAdvanced) {
                                    OutlinedTextField(
                                        value = backendUrl,
                                        onValueChange = { backendUrl = it },
                                        label = { Text("Backend URL") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (accessToken == null) {
                                        Button(onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                runCatching {
                                                    ApiClient(backendUrl.trim().trimEnd('/')).login(email.trim())
                                                }.onSuccess { token ->
                                                    withContext(Dispatchers.Main) {
                                                        AppState.setAccessToken(context, token)
                                                        accessToken = token
                                                        events.add(0, "Signed in as $email")
                                                    }
                                                }.onFailure { t ->
                                                    withContext(Dispatchers.Main) {
                                                        val msg = t.message ?: t.toString()
                                                        events.add(0, "Sign-in failed: ${t::class.java.simpleName}: $msg")
                                                    }
                                                }
                                            }
                                        }) {
                                            Text("Sign in")
                                        }
                                    } else {
                                        TextButton(onClick = {
                                            AppState.setAccessToken(context, null)
                                            accessToken = null
                                            events.add(0, "Signed out")
                                        }) {
                                            Text("Sign out")
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Device: ${AppState.deviceId(context).take(8)}…", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    1 -> DelegationScreen(
                        accessToken = accessToken,
                        backendUrl = backendUrl,
                        onEvent = { event -> events.add(0, event) }
                    )
                    2 -> VisitorScreen(
                        accessToken = accessToken,
                        backendUrl = backendUrl,
                        onEvent = { event -> events.add(0, event) }
                    )
                    3 -> TimeTrackingCard(
                        backendUrl = backendUrl,
                        accessToken = accessToken,
                        scope = scope
                    )
                }

                // Recent events card removed - keeping events internal only
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AccessCard(
    gateId: String, 
    signedIn: Boolean, 
    accessToken: String?, 
    backendUrl: String,
    onEvent: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var qrContent by remember { mutableStateOf("") }
    var tokenExpiry by remember { mutableStateOf(0L) }
    
    // Auto-refresh QR code every 25 seconds if signed in
    LaunchedEffect(signedIn, accessToken, gateId, backendUrl) {
        if (!signedIn || accessToken == null || gateId.isBlank()) {
            qrBitmap = null
            qrContent = ""
            return@LaunchedEffect
        }
        
        while (signedIn && accessToken != null) {
            try {
                isGenerating = true
                val readerNonce = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
                val deviceId = AppState.deviceId(context)
                
                withContext(Dispatchers.IO) {
                    val response = ApiClient(backendUrl.trim().trimEnd('/')).issueQrToken(
                        accessToken = accessToken,
                        gateId = gateId,
                        readerNonce = readerNonce,
                        deviceId = deviceId
                    )
                    
                    tokenExpiry = response.expEpochSeconds
                    qrContent = response.token
                    
                    val bitmap = QRCodeGenerator.generateQRCode(response.token, 300)
                    withContext(Dispatchers.Main) {
                        qrBitmap = bitmap
                        isGenerating = false
                        onEvent("QR code generated for $gateId")
                    }
                }
                
                delay(25000) // Refresh every 25 seconds
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    onEvent("QR generation failed: ${e.message}")
                }
                delay(5000) // Retry in 5 seconds on error
            }
        }
    }
    
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Access Card", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Gate: $gateId", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                !signedIn -> {
                    Text(
                        "Sign in to generate QR access codes.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                isGenerating -> {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Generating QR code...", style = MaterialTheme.typography.bodySmall)
                }
                qrBitmap != null -> {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Access Code",
                        modifier = Modifier
                            .size(120.dp)
                            .aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scan at reader",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        "Unable to generate QR code",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TimeTrackingCard(
    backendUrl: String,
    accessToken: String?,
    scope: CoroutineScope
) {
    var currentSession by remember { mutableStateOf<TimeSessionResponse?>(null) }
    var summary by remember { mutableStateOf<TimeSummaryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentDuration by remember { mutableStateOf(0) }

    // Fetch data every 5 seconds if signed in
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
                            error = t.message
                            loading = false
                        }
                    }
                }
                delay(5000)
            }
        } else {
            loading = false
        }
    }

    // Update current duration every second if there's an active session
    LaunchedEffect(currentSession, summary) {
        val session = currentSession
        val summaryData = summary
        if (session != null && summaryData?.hasActiveSession == true) {
            while (true) {
                val entryTime = try {
                    java.time.Instant.parse(session.entryTime)
                } catch (e: Exception) {
                    null
                }
                if (entryTime != null) {
                    val now = java.time.Instant.now()
                    currentDuration = java.time.Duration.between(entryTime, now).seconds.toInt()
                }
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Time Tracking",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (accessToken == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Please sign in first",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            // Current Session Card
            val session = currentSession
            val summaryData = summary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summaryData?.hasActiveSession == true) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Current Session",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (session != null && summaryData?.hasActiveSession == true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status:", style = MaterialTheme.typography.bodyMedium)
                            Text("Active", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entry Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                session.entryTime.substring(11, 19),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatDuration(currentDuration),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            "No active session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Card
            if (summaryData != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Summary",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Sessions:", style = MaterialTheme.typography.bodyMedium)
                            Text("${summaryData.totalSessions}", fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Time:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                summaryData.totalTimeFormatted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (summaryData.averageTimeFormatted.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Average:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    summaryData.averageTimeFormatted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, secs)
        minutes > 0 -> String.format("%dm %02ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}


