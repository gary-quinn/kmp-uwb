package com.atruedev.kmpuwb.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atruedev.kmpuwb.config.RangingRole

@Composable
fun RangingDemoScreen() {
    var selectedRole by remember { mutableStateOf<RangingRole?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val role = selectedRole) {
                null -> RoleSelectionScreen(onRoleSelected = { selectedRole = it })
                else -> RangingScreen(role = role)
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(onRoleSelected: (RangingRole) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "kmp-uwb",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Select ranging role",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp),
        )
        Button(
            onClick = { onRoleSelected(RangingRole.CONTROLLER) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Controller")
        }
        OutlinedButton(
            onClick = { onRoleSelected(RangingRole.CONTROLEE) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
        ) {
            Text("Controlee")
        }
        Text(
            text =
                "Controller initiates the session.\n" +
                    "Controlee responds to a controller.\n\n" +
                    "Run this app on two UWB devices,\n" +
                    "one as Controller and one as Controlee.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun RangingScreen(role: RangingRole) {
    val scope = rememberCoroutineScope()
    val demo = remember(role) { RangingDemo(scope, role) }
    val log by demo.log.collectAsState()
    val phase by demo.phase.collectAsState()
    val localParams by demo.localParamsBase64.collectAsState()

    DisposableEffect(demo) {
        demo.start()
        onDispose { demo.stop() }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Ranging Demo",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = role.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (phase == DemoPhase.WAITING_FOR_REMOTE_PARAMS) {
            ParamsExchangeCard(
                localParams = localParams,
                onRemoteParamsSubmitted = { demo.submitRemoteParams(it) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = log.ifEmpty { "Initializing..." },
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ParamsExchangeCard(
    localParams: String,
    onRemoteParamsSubmitted: (String) -> Unit,
) {
    var remoteInput by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Step 1: Send to the other device",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "Tap Copy, then send via AirDrop or any messaging app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            OutlinedButton(
                onClick = {
                    copyToClipboard(localParams)
                    copied = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = localParams.isNotEmpty(),
            ) {
                Text(if (copied) "Copied ✓" else "Copy Local Params")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Step 2: Paste from the other device",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "Copy the other device's params and paste here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = remoteInput,
                onValueChange = { remoteInput = it },
                modifier =
                    Modifier
                        .fillMaxWidth(),
                placeholder = { Text("Paste remote params") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )

            Button(
                onClick = { onRemoteParamsSubmitted(remoteInput) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                enabled = remoteInput.isNotBlank(),
            ) {
                Text("Start Ranging")
            }
        }
    }
}
