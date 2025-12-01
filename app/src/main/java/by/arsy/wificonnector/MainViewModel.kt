package by.arsy.wificonnector

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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


private const val SERVICE_ID = "by.arsy.wificonnector"
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val connectionsClient = Nearby.getConnectionsClient(application)
    private var connectEndpointId = ""
    private val _text = MutableStateFlow("")
    val text = _text.asStateFlow()

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
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {}
        override fun onDisconnected(p0: String) {}
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(
            endpointId: String,
            discoveredEndpointInfo: DiscoveredEndpointInfo
        ) {
            connectionsClient.requestConnection("Multiplayer", endpointId, connectionCallback)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    fun createEndpoint() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startAdvertising(
            "WiFiConnector",
            SERVICE_ID,
            connectionCallback,
            options
        )
    }

    fun discoveryEndpoint() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("discoveryEndpoint", "Success") }
            .addOnFailureListener { e -> Log.e("discoveryEndpoint", "Failure", e) }
    }

    fun sendText(text: String, toEndpointId: String) {
        val bytesPayload = Payload.fromBytes(text.toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(toEndpointId, bytesPayload)
    }

    fun updateText(text: String) {
        sendText(text, connectEndpointId)
        _text.value = text
    }
}