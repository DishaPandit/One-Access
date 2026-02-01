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
                                Text("Settings", fontWeight = FontWeight.SemiBold)

                                OutlinedTextField(
                                    value = backendUrl,
                                    onValueChange = { backendUrl = it },
                                    label = { Text("Backend URL") },
                                    supportingText = { Text("Emulator: http://10.0.2.2:8000 | Phone: http://<your-laptop-ip>:8000") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email (demo)") },
                                    supportingText = { Text("Try: alice@acme.com or bob@globex.com") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = gateId,
                                    onValueChange = { gateId = it },
                                    label = { Text("Gate ID") },
                                    supportingText = { Text("MAIN_GATE, BLD_ACME, BLD_GLOBEX") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

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
                    3 -> TimeTrackingScreen(
                        backendUrl = backendUrl,
                        accessToken = accessToken,
                        onEvent = { event -> events.add(0, event) },
                        context = context,
                        scope = scope
                    )
                }

                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recent", fontWeight = FontWeight.SemiBold)
                        if (events.isEmpty()) {
                            Text("No events yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            events.take(6).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
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
                        "Scan QR code at reader\n(Auto-refreshes every 25s)",
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


