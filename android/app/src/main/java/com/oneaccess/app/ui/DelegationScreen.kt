package com.oneaccess.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneaccess.app.net.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DelegationScreen(
    accessToken: String?,
    backendUrl: String,
    onEvent: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var delegateeEmail by remember { mutableStateOf("") }
    var selectedGates by remember { mutableStateOf(mutableSetOf<String>()) }
    var delegationHours by remember { mutableStateOf("24") }
    val availableGates = listOf("MAIN_GATE", "BLD_ACME", "BLD_GLOBEX")

    if (accessToken == null) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Delegate Access", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sign in to delegate your access to other employees.", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Delegate Access", fontWeight = FontWeight.SemiBold)
            Text("Give temporary access to another employee", style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = delegateeEmail,
                onValueChange = { delegateeEmail = it },
                label = { Text("Employee Email") },
                supportingText = { Text("Email of the employee to grant access to") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Gates to Allow:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            availableGates.forEach { gate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedGates.contains(gate),
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedGates.add(gate)
                            } else {
                                selectedGates.remove(gate)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(gate)
                }
            }

            OutlinedTextField(
                value = delegationHours,
                onValueChange = { delegationHours = it },
                label = { Text("Valid for (hours)") },
                supportingText = { Text("1-168 hours (max 1 week)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (delegateeEmail.isBlank() || selectedGates.isEmpty() || delegationHours.isBlank()) {
                        onEvent("Please fill all delegation fields")
                        return@Button
                    }
                    
                    val hours = delegationHours.toIntOrNull()
                    if (hours == null || hours < 1 || hours > 168) {
                        onEvent("Hours must be between 1 and 168")
                        return@Button
                    }

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            ApiClient(backendUrl.trim().trimEnd('/')).createDelegation(
                                accessToken = accessToken,
                                delegateeEmail = delegateeEmail.trim(),
                                gateIds = selectedGates.toList(),
                                hours = hours
                            )
                        }.onSuccess { delegationId ->
                            withContext(Dispatchers.Main) {
                                onEvent("Delegation created: $delegationId")
                                delegateeEmail = ""
                                selectedGates.clear()
                                delegationHours = "24"
                            }
                        }.onFailure { t ->
                            withContext(Dispatchers.Main) {
                                onEvent("Delegation failed: ${t.message}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Delegation")
            }
        }
    }
}