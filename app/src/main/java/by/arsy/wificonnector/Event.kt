package by.arsy.wificonnector

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


object EventBus {
    private val _channel = Channel<Event>(Channel.BUFFERED)
    val channel = _channel.receiveAsFlow()

    suspend fun send(event: Event) {
        _channel.send(element = event)
    }
}
sealed class Event {
    data class Progress(val message: String) : Event()
    object CompleteProgress : Event()
}