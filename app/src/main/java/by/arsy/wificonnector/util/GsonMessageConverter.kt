package by.arsy.wificonnector.util

import by.arsy.wificonnector.model.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GsonMessageConverter {
    private val gson = Gson()

    fun toJson(message: Message): String {
        return gson.toJson(message)
    }

    fun fromJson(json: String): Message {
        return gson.fromJson(json, Message::class.java)
    }

    fun listToJson(message: List<Message>): String {
        return gson.toJson(message)
    }

    fun listFromJson(json: String): List<Message> {
        val type = object : TypeToken<List<Message>>() {}.type
        return gson.fromJson(json, type)
    }
}