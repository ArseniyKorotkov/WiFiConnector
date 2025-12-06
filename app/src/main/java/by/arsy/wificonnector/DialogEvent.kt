package by.arsy.wificonnector

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


object EventBus {
    private val _channel = Channel<DialogEvent>(Channel.BUFFERED)
    val channel = _channel.receiveAsFlow()

    suspend fun send(event: DialogEvent) {
        _channel.send(element = event)
    }
}
sealed class DialogEvent {
    data class ShowDialog(
        val message: String,
        val onClickOk: () -> Unit,
        val onClickCancel: () -> Unit,
    ) : DialogEvent()
    object HideDialog : DialogEvent()
}