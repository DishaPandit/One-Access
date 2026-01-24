package com.oneaccess.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneaccess.app.net.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccessScreen(
    gateId: String,
    onGateIdChange: (String) -> Unit,
    backendUrl: String,
    onBackendUrlChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    accessToken: String?,
    onAccessTokenChange: (String?) -> Unit,
    onEvent: (String) -> Unit,
    context: Context,
    scope: CoroutineScope
) {
    AccessCard(
        gateId = gateId,
        signedIn = accessToken != null,
        accessToken = accessToken,
        backendUrl = backendUrl,
        onEvent = onEvent
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settings", fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = backendUrl,
                onValueChange = onBackendUrlChange,
                label = { Text("Backend URL") },
                supportingText = { Text("Emulator: http://10.0.2.2:8000 | Phone: http://<your-laptop-ip>:8000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email (demo)") },
                supportingText = { Text("Try: alice@acme.com or bob@globex.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = gateId,
                onValueChange = onGateIdChange,
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
                                    onAccessTokenChange(token)
                                    onEvent("Signed in as $email")
                                }
                            }.onFailure { t ->
                                withContext(Dispatchers.Main) {
                                    val msg = t.message ?: t.toString()
                                    onEvent("Sign-in failed: ${t::class.java.simpleName}: $msg")
                                }
                            }
                        }
                    }) {
                        Text("Sign in")
                    }
                } else {
                    TextButton(onClick = {
                        AppState.setAccessToken(context, null)
                        onAccessTokenChange(null)
                        onEvent("Signed out")
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