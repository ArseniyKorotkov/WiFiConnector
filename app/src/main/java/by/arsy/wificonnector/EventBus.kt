package by.arsy.wificonnector

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object EventBus {
    private val _channel = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val channel = _channel.asSharedFlow()

    suspend fun emit(event: Event) {
        _channel.emit(value = event)
    }
}

sealed class Event
sealed class DialogEvent : Event() {
    data class ShowDialog(
        val message: String,
        val onClickOk: () -> Unit,
        val onClickCancel: () -> Unit,
    ) : DialogEvent()

    object HideDialog : DialogEvent()
}

sealed class NavigateEvent : Event() {
    object BackStack : NavigateEvent()
}