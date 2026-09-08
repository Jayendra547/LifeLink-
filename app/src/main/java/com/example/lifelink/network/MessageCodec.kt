package com.example.lifelink.network

import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import org.json.JSONArray
import org.json.JSONObject

object MessageCodec {

    fun encode(message: LifeLinkMessage): ByteArray {
        val json = JSONObject().apply {
            put("id", message.id)
            put("senderId", message.senderId)
            put("text", message.text)
            put("language", message.language)
            put("intent", message.intent.name)
            put("priority", message.priority)
            put("timestamp", message.timestamp)
            put("hopCount", message.hopCount)
            put("ttl", message.ttl)

            val pathArray = JSONArray()
            message.path.forEach { pathArray.put(it) }
            put("path", pathArray)

            if (message.translatedText != null) {
                put("translatedText", message.translatedText)
            }
            if (message.targetLanguage != null) {
                put("targetLanguage", message.targetLanguage)
            }
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(data: ByteArray): LifeLinkMessage {
        val jsonString = String(data, Charsets.UTF_8)
        val json = JSONObject(jsonString)

        val intentStr = json.optString("intent", EmergencyIntent.GENERAL.name)
        val intent = try {
            EmergencyIntent.valueOf(intentStr)
        } catch (_: Exception) {
            EmergencyIntent.GENERAL
        }

        val pathList = mutableListOf<String>()
        val pathArray = json.optJSONArray("path")
        if (pathArray != null) {
            for (i in 0 until pathArray.length()) {
                pathList.add(pathArray.getString(i))
            }
        }

        val translatedText = if (json.has("translatedText")) json.getString("translatedText") else null
        val targetLanguage = if (json.has("targetLanguage")) json.getString("targetLanguage") else null

        return LifeLinkMessage(
            id = json.getString("id"),
            senderId = json.getString("senderId"),
            text = json.getString("text"),
            language = json.getString("language"),
            intent = intent,
            priority = json.getInt("priority"),
            timestamp = json.getLong("timestamp"),
            hopCount = json.optInt("hopCount", 0),
            ttl = json.optInt("ttl", 5),
            path = pathList,
            translatedText = translatedText,
            targetLanguage = targetLanguage
        )
    }

    fun prettyPrint(message: LifeLinkMessage): String {
        return try {
            val raw = String(encode(message), Charsets.UTF_8)
            JSONObject(raw).toString(2)
        } catch (_: Exception) {
            message.toString()
        }
    }
}
