package com.atruedev.kmpuwb.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atruedev.kmpuwb.config.RangingRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Composable
fun RangingDemoScreen() {
    var selectedRole by remember { mutableStateOf<RangingRole?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (selectedRole == null) {
                RoleSelectionScreen(onRoleSelected = { selectedRole = it })
            } else {
                RangingScreen(role = selectedRole!!)
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
            text = "Controller initiates the session.\nControlee responds to a controller.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun RangingScreen(role: RangingRole) {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val demo = remember { RangingDemo(scope, role) }
    val log by demo.log.collectAsState()

    DisposableEffect(Unit) {
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
        Text(
            text = log.ifEmpty { "Initializing..." },
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
