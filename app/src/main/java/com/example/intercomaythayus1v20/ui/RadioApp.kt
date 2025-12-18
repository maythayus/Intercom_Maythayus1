package com.example.intercomaythayus1v20.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RadioApp(
    isUsbConnected: () -> Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    sosStatus: String?,
    onSosConfirm: (testMode: Boolean) -> Unit,
    onCallEmergencyClick: () -> Unit,
    onShareEmergencyClick: () -> Unit,
    bleStatus: String?,
    heartRateBpm: Int?,
    onBleConnectClick: () -> Unit,
    onBleDisconnectClick: () -> Unit,
) {
    val showConfirm = remember { mutableStateOf(false) }
    val testMode = remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (sosStatus != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                ) {
                    Text(
                        text = sosStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (bleStatus != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                ) {
                    Text(
                        text = bleStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = if (isUsbConnected()) "USB connecté" else "USB déconnecté",
                style = MaterialTheme.typography.titleMedium,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PTT",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Maintenir pour émettre",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val buttonColor = if (isUsbConnected()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Mode test (sans émission)")
                Switch(
                    checked = testMode.value,
                    onCheckedChange = { testMode.value = it },
                )

                Button(
                    onClick = { showConfirm.value = true },
                    enabled = isUsbConnected(),
                ) {
                    Text(text = "SOS")
                }

                Button(onClick = onCallEmergencyClick) {
                    Text(text = "Appeler urgences")
                }

                Button(onClick = onShareEmergencyClick) {
                    Text(text = "Partager message")
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Bague (BLE)")
                Text(
                    text = heartRateBpm?.let { "FC: $it bpm" } ?: "FC: --",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onBleConnectClick) {
                    Text(text = "Connecter")
                }
                Button(onClick = onBleDisconnectClick) {
                    Text(text = "Déconnecter")
                }
            }

            if (showConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showConfirm.value = false },
                    title = { Text(text = "Confirmer SOS") },
                    text = { Text(text = "Confirmer émission SOS sur 156.800 MHz ?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirm.value = false
                                onSosConfirm(testMode.value)
                            }
                        ) {
                            Text(text = if (testMode.value) "Tester" else "Émettre")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirm.value = false }) {
                            Text(text = "Annuler")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .pointerInput(isUsbConnected()) {
                        detectTapGestures(
                            onPress = {
                                if (isUsbConnected()) {
                                    onPttDown()
                                    try {
                                        awaitRelease()
                                    } finally {
                                        onPttUp()
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Appuyer",
                    color = if (isUsbConnected()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
