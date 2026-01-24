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
fun VisitorScreen(
    accessToken: String?,
    backendUrl: String,
    onEvent: (String) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var visitorName by remember { mutableStateOf("") }
    var visitorPhone by remember { mutableStateOf("") }
    var selectedGates by remember { mutableStateOf(mutableSetOf<String>()) }
    var visitorHours by remember { mutableStateOf("8") }
    val availableGates = listOf("MAIN_GATE", "BLD_ACME", "BLD_GLOBEX")

    if (accessToken == null) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Visitor Passes", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sign in to create temporary visitor passes.", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create Visitor Pass", fontWeight = FontWeight.SemiBold)
            Text("Generate temporary access for visitors", style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = visitorName,
                onValueChange = { visitorName = it },
                label = { Text("Visitor Name") },
                supportingText = { Text("Full name of the visitor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = visitorPhone,
                onValueChange = { visitorPhone = it },
                label = { Text("Visitor Phone") },
                supportingText = { Text("Phone number for identification") },
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
                value = visitorHours,
                onValueChange = { visitorHours = it },
                label = { Text("Valid for (hours)") },
                supportingText = { Text("1-72 hours (max 3 days for visitors)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (visitorName.isBlank() || visitorPhone.isBlank() || selectedGates.isEmpty() || visitorHours.isBlank()) {
                        onEvent("Please fill all visitor pass fields")
                        return@Button
                    }
                    
                    val hours = visitorHours.toIntOrNull()
                    if (hours == null || hours < 1 || hours > 72) {
                        onEvent("Hours must be between 1 and 72")
                        return@Button
                    }

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            ApiClient(backendUrl.trim().trimEnd('/')).createVisitorPass(
                                accessToken = accessToken,
                                visitorName = visitorName.trim(),
                                visitorPhone = visitorPhone.trim(),
                                gateIds = selectedGates.toList(),
                                hours = hours
                            )
                        }.onSuccess { passId ->
                            withContext(Dispatchers.Main) {
                                onEvent("Visitor pass created: $passId")
                                visitorName = ""
                                visitorPhone = ""
                                selectedGates.clear()
                                visitorHours = "8"
                            }
                        }.onFailure { t ->
                            withContext(Dispatchers.Main) {
                                onEvent("Visitor pass failed: ${t.message}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Visitor Pass")
            }
        }
    }
}