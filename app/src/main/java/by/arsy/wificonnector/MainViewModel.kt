package by.arsy.wificonnector

import android.app.Application
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val SERVICE_ID = "by.arsy.wificonnector"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val name = "Nick${(Math.random() * 1000).toInt()}"
    private val connectionsClient = Nearby.getConnectionsClient(application)

    private val connectEndpointIdSet = mutableSetOf<String>()
    private val _text = MutableStateFlow("")
    val text = _text.asStateFlow()
    private val _discoveryState = MutableStateFlow(false)
    private val _host = MutableStateFlow(false)
    val discoveryState = _discoveryState.asStateFlow()
    val host = _host.asStateFlow()
    val discoveredEndpointSet = mutableStateSetOf<Endpoint>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                _text.value = String(bytes = payload.asBytes()!!)
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {}
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        val requestToConnectEndpointMap = mutableMapOf<String, String>()
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            requestToConnectEndpointMap[endpointId] = connectionInfo.endpointName
            if (connectionInfo.isIncomingConnection) {
                sendDialogEvent(
                    message = "Do you want to connect with ${requestToConnectEndpointMap[endpointId]}",
                    onClickOk = {
                        connectionsClient.acceptConnection(endpointId, payloadCallback)
                        resetDialogEvent()
                    },
                    onClickCancel = {
                        connectionsClient.rejectConnection(endpointId)
                        resetDialogEvent()
                    }
                )
            } else {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                sendDialogEvent(
                    message = "Wait decision from ${requestToConnectEndpointMap[endpointId]}",
                )
            }
        }

        override fun onConnectionResult(
            endpointId: String,
            connectionResolution: ConnectionResolution
        ) {
            if (connectionResolution.status.isSuccess) {
                sendDialogEvent(
                    message = "Successfully connect with ${requestToConnectEndpointMap[endpointId]}",
                    onClickOk = {
                        connectEndpointIdSet.add(endpointId)
                        viewModelScope.launch {
                            EventBus.emit(NavigateEvent.NavigateTo(Route.Chat.route))
                        }
                    }
                )
            } else {
                sendDialogEvent(
                    message = "Reject connect with ${requestToConnectEndpointMap[endpointId]}"
                )
            }
        }

        override fun onDisconnected(endpointId: String) {
            sendDialogEvent(
                message = "Disconnect with ${requestToConnectEndpointMap[endpointId]}",
                onClickOk = {
                    viewModelScope.launch {
                        EventBus.emit(NavigateEvent.BackStack)
                    }
                }
            )
            requestToConnectEndpointMap.remove(endpointId)
            discoveredEndpointSet.removeIf { it.endpointId == endpointId }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
            endpointId: String,
            discoveredEndpointInfo: DiscoveredEndpointInfo
        ) {
            discoveredEndpointSet.add(
                Endpoint(endpointId, discoveredEndpointInfo.endpointName)
            )
        }

        override fun onEndpointLost(endpointId: String) {
            discoveredEndpointSet.removeIf { it.endpointId == endpointId }
        }
    }

    fun createEndpoint() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startAdvertising(
            name,
            SERVICE_ID,
            connectionCallback,
            options
        )
        _host.value = true
        sendDialogEvent(
            message = "Created endpoint with name $name"
        )
    }

    fun destroyEndpoint() {
        if (_host.value) {
            connectionsClient.stopAdvertising()
        }
    }

    fun discoveryEndpoint() {
        connectionsClient.stopDiscovery()
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                _discoveryState.value = true
            }
            .addOnFailureListener { _ ->
                sendDialogEvent(
                    message = "Turn on wifi and location"
                )
                _discoveryState.value = false
            }
    }

    fun stopDiscoveryEndpoint() {
        connectionsClient.stopDiscovery()
        _discoveryState.value = false
    }

    fun requestConnection(endpointId: String) {
        connectionsClient.requestConnection(name, endpointId, connectionCallback)
    }

    fun updateText(text: String) {
        connectEndpointIdSet.forEach {
            sendText(text, toEndpointId = it)
        }
        _text.value = text
    }

    private fun sendText(text: String, toEndpointId: String) {
        val bytesPayload = Payload.fromBytes(text.toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(toEndpointId, bytesPayload)
    }

    private fun sendDialogEvent(
        message: String,
        onClickOk: () -> Unit = { resetDialogEvent() },
        onClickCancel: () -> Unit = { resetDialogEvent() },
    ) {
        viewModelScope.launch {
            EventBus.emit(
                event = DialogEvent.ShowDialog(
                    message = message,
                    onClickOk = onClickOk,
                    onClickCancel = onClickCancel
                )
            )
        }
    }

    private fun resetDialogEvent() {
        viewModelScope.launch {
            EventBus.emit(DialogEvent.HideDialog)
        }
    }
}

data class Endpoint(val endpointId: String, val endpointName: String)