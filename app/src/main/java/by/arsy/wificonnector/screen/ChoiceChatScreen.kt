package by.arsy.wificonnector.screen


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import by.arsy.wificonnector.DialogEvent
import by.arsy.wificonnector.EventBus
import by.arsy.wificonnector.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ChoiceChatScreen(
    viewModel: MainViewModel,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveredEndpointSet = viewModel.discoveredEndpointSet
    val discoveryState by viewModel.discoveryState.collectAsState()
    val scope = rememberCoroutineScope()
    var userName by rememberSaveable { mutableStateOf(viewModel.name) }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text("Username:")
        TextField(
            modifier = Modifier.padding(bottom = 6.dp),
            value = userName,
            onValueChange = {
                viewModel.updateUsername(username = it)
                userName = it
            }
        )

        Button(onClick = {
            viewModel.createEndpoint()
            onNavigate()
        }) {
            Text(text = "Create endpoint")
        }

        Button(onClick = if (discoveryState) viewModel::stopDiscoveryEndpoint else viewModel::discoveryEndpoint) {
            Text(text = if (discoveryState) "stopDiscoveryEndpoint" else "discoveryEndpoint")
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(discoveredEndpointSet.toList()) {
                    Text(
                        text = it.endpointName,
                        modifier = Modifier
                            .clickable(onClick = {
                                viewModel.requestConnection(it.endpointId)
                                viewModel.stopDiscoveryEndpoint()
                                scope.launch {
                                    EventBus.emit(
                                        DialogEvent.ShowDialog(
                                            message = "Wait connect with ${it.endpointName}",
                                            onClickOk = {},
                                            onClickCancel = {})
                                    )
                                }
                            })
                    )
                }
            }

            if (discoveryState) {
                CircularProgressIndicator(
                    modifier = Modifier.align(alignment = Alignment.TopEnd),
                    color = Color(0xE6FFFFFF)
                )
            }
        }
    }
}