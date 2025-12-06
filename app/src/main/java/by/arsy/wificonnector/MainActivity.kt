package by.arsy.wificonnector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.savedstate.savedState
import by.arsy.wificonnector.screen.StartScreen
import by.arsy.wificonnector.ui.theme.DialogScreen
import by.arsy.wificonnector.ui.theme.WiFiConnectorTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        enableEdgeToEdge()
        setContent {
            WiFiConnectorTheme {
                val stateMessage by mainViewModel.stateMessage.collectAsState()
                var primitiveNavigate by rememberSaveable { mutableStateOf(true) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (primitiveNavigate) {
                        StartScreen(
                            viewModel = mainViewModel,
                            onNavigate = { primitiveNavigate = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    } else {
                        Greeting(
                            onNavigate = { primitiveNavigate = true },
                            viewModel = mainViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }

                    if (stateMessage.isNotBlank()) {
                        DialogScreen(
                            message = stateMessage,
                            onClose = { mainViewModel.resetState() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }

        val notGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(input = notGranted.toTypedArray())
        }
    }
}

@Composable
fun Greeting(
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
            onValueChange =  viewModel::updateText
        )

        Text(
            text = text
        )
    }
}