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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


private const val SERVICE_ID = "by.arsy.wificonnector"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val name = "Nick${(Math.random() * 1000).toInt()}"
    private val connectionsClient = Nearby.getConnectionsClient(application)
    private var connectEndpointId = ""
    private val _text = MutableStateFlow("")
    val text = _text.asStateFlow()
    private val _stateMessage = MutableStateFlow("")
    private val _connectionFlag = MutableStateFlow(true)
    private val _discoveryState = MutableStateFlow(false)
    private val _host = MutableStateFlow(false)
    val stateMessage = _stateMessage.asStateFlow()
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
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            connectEndpointId = endpointId
            _connectionFlag.value = true
            changeState("want to connect with ${connectionInfo.endpointName}?")
            viewModelScope.launch {
                _stateMessage.first { it.isBlank() && _connectionFlag.value }
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                changeState("connect with ${connectionInfo.endpointName}")
                _connectionFlag.value = false
            }
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {}
        override fun onDisconnected(p0: String) {}
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
        changeState("create endpoint with name $name")
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
                changeState("On wifi. Sometimes help on localization too")
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

    fun sendText(text: String, toEndpointId: String) {
        val bytesPayload = Payload.fromBytes(text.toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(toEndpointId, bytesPayload)
    }

    fun updateText(text: String) {
        sendText(text, connectEndpointId)
        _text.value = text
    }

    fun changeState(message: String) {
        _stateMessage.value = message
    }

    fun resetState() {
        changeState("")
    }
}

data class Endpoint(val endpointId: String, val endpointName: String)