package by.arsy.wificonnector.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import by.arsy.wificonnector.DialogEvent
import by.arsy.wificonnector.EventBus

@Composable
fun DialogScreen(
    modifier: Modifier = Modifier
) {
    var dialogType by remember { mutableStateOf(DialogType()) }
    var visible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EventBus.channel.collect { alertEvent ->
            when (alertEvent) {
                is DialogEvent.ShowDialog -> {
                    dialogType = dialogType.copy(
                        message = alertEvent.message,
                        onClickOk = alertEvent.onClickOk,
                        onClickCancel = alertEvent.onClickCancel
                    )
                    visible = true
                }

                is DialogEvent.HideDialog -> {
                    visible = false
                }
            }
        }
    }

    AnimatedVisibility(visible) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(Color(0xED3A481B))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = dialogType.message)

                Button(onClick = dialogType.onClickOk) {
                    Text("ok")
                }

                Button(onClick = dialogType.onClickCancel) {
                    Text("cancel")
                }
            }
        }
    }
}

private data class DialogType(
    val message: String = "",
    val onClickOk: () -> Unit = {},
    val onClickCancel: () -> Unit = {},
)