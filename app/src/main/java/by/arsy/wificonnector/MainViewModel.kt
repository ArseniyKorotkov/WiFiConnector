package by.arsy.wificonnector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _text = MutableStateFlow("")
    val text = _text.asStateFlow()

    fun updateText(text: String) {
        _text.value = text
    }
}