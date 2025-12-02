package by.arsy.wificonnector.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DialogScreen(
    message: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color(0xED3A481B))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = message)

            Button(onClick = onClose) {
                Text("ok")
            }
        }
    }
}