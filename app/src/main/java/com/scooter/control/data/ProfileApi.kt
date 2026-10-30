package com.scooter.control.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Загружает профиль и аватарку с сервера бота (/api/profile, /api/avatar). */
object ProfileApi {

    suspend fun refresh(context: Context, session: SessionStore): Boolean =
        withContext(Dispatchers.IO) {
            val token = session.tokenFlow.first()
            val current = session.profileFlow.first()
            if (token.isNullOrBlank() || current.server.isBlank()) return@withContext false
            try {
                val base = current.server.trimEnd('/')
                val q = URLEncoder.encode(token, "UTF-8")
                val json = JSONObject(String(get("$base/api/profile?token=$q"), Charsets.UTF_8))
                var stamp = current.avatarStamp
                if (json.optBoolean("has_avatar", false)) {
                    session.avatarFile().writeBytes(get("$base/api/avatar?token=$q"))
                    stamp = System.currentTimeMillis()
                }
                session.updateProfile(
                    current.copy(
                        name = json.optString("name", current.name),
                        username = json.optString("username", current.username),
                        uid = json.optString("id", current.uid),
                        avatarStamp = stamp,
                    )
                )
                true
            } catch (e: Exception) {
                false
            }
        }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
