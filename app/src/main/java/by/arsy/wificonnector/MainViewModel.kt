package by.arsy.wificonnector

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateSetOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.arsy.wificonnector.model.Message
import by.arsy.wificonnector.util.GsonMessageConverter
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
    private val sharedPreferences = getApplication<Application>().getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )
    private val sharedPreferencesUsernameKey = "username"
    private val androidId = Settings.Secure.getString(
        getApplication<Application>().contentResolver,
        Settings.Secure.ANDROID_ID
    )
    private val _chatList = MutableStateFlow(emptyList<Message>())
    private val connectionsClient = Nearby.getConnectionsClient(application)
    private val connectEndpointIdSet = mutableSetOf<String>()
    private val _discoveryState = MutableStateFlow(false)
    private var isHost = false

    val chatList = _chatList.asStateFlow()
    val discoveryState = _discoveryState.asStateFlow()
    val discoveredEndpointSet = mutableStateSetOf<Endpoint>()

    var name = getUsername()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val json = String(payload.asBytes()!!)
                if (isHost) {
                    val message = GsonMessageConverter.fromJson(json)
                    hostDistributionCorrectChatList(message)
                } else {
                    _chatList.value = GsonMessageConverter.listFromJson(json)
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {}
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        val requestToConnectEndpointMap = mutableMapOf<String, String>()
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            requestToConnectEndpointMap[endpointId] = connectionInfo.endpointName
            isHost = connectionInfo.isIncomingConnection
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
                        if (isHost) {
                            distributionChatListBytes()
                        }
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
            if (!isHost) {
                sendDialogEvent(
                    message = "Disconnect with ${requestToConnectEndpointMap[endpointId]}",
                    onClickOk = {
                        viewModelScope.launch {
                            EventBus.emit(NavigateEvent.BackStack)
                        }
                    }
                )
            }
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
        isHost = true
        sendDialogEvent(
            message = "Created endpoint with name $name"
        )
    }

    fun destroyEndpoint() {
        if (isHost) {
            connectionsClient.stopAdvertising()
        }
        connectEndpointIdSet.forEach {
            connectionsClient.disconnectFromEndpoint(it)
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

    fun updateUsername(username: String) {
        name = username
        sharedPreferences.edit { putString(sharedPreferencesUsernameKey, username) }
    }

    fun sendMessage(text: String) {
        val message = Message(
            id = androidId,
            username = name,
            text = text
        )

        if (isHost) {
            hostDistributionCorrectChatList(message)
        } else {
            distributionBytes(text = GsonMessageConverter.toJson(message))
        }
    }

    fun isOwnerId(id: String) : Boolean {
        return id == androidId
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

    private fun getUsername(): String {
        val username = sharedPreferences.getString(sharedPreferencesUsernameKey, null)
            ?: "Nick${(Math.random() * 1000).toInt()}"
        return username
    }

    private fun hostDistributionCorrectChatList(message: Message) {
        hostUpdateChatList(message)
        distributionChatListBytes()
    }

    private fun distributionChatListBytes() {
        distributionBytes(text = GsonMessageConverter.listToJson(_chatList.value))
    }

    private fun distributionBytes(text: String) {
        connectEndpointIdSet.forEach {
            val bytesPayload = Payload.fromBytes(text.toByteArray(Charsets.UTF_8))
            connectionsClient.sendPayload(it, bytesPayload)
        }
    }

    private fun hostUpdateChatList(message: Message) {
        val mutableChat = _chatList.value.toMutableList()
        mutableChat.add(message)
        _chatList.value = mutableChat.map {
            if (it.id == message.id) {
                it.copy(username = message.username)
            } else {
                it
            }
        }
    }
}

data class Endpoint(val endpointId: String, val endpointName: String)