package by.arsy.wificonnector.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import by.arsy.wificonnector.MainViewModel

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {

    val text by viewModel.text.collectAsState()

    BackHandler {
        viewModel.destroyEndpoint()
        onNavigate()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        TextField(
            value = text,
            onValueChange = viewModel::updateText
        )

        Text(
            text = text
        )
    }
}