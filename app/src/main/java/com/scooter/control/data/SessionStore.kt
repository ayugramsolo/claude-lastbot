package com.scooter.control.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "session")
private val KEY_TOKEN = stringPreferencesKey("auth_token")
private val KEY_NAME = stringPreferencesKey("profile_name")
private val KEY_USERNAME = stringPreferencesKey("profile_username")
private val KEY_UID = stringPreferencesKey("profile_uid")
private val KEY_SERVER = stringPreferencesKey("profile_server")
private val KEY_AVATAR = longPreferencesKey("profile_avatar_stamp")

/** Данные, которые приходят из Telegram-бота. */
data class Profile(
    val name: String = "",
    val username: String = "",
    val uid: String = "",
    val server: String = "",
    val avatarStamp: Long = 0L,
)

/** Deep-link: scooterapp://auth?token=..&name=..&username=..&uid=..&server=.. */
data class DeepLink(
    val token: String,
    val name: String = "",
    val username: String = "",
    val uid: String = "",
    val server: String = "",
)

/**
 * Хранит токен и профиль, полученные из Telegram-бота.
 * Токен пока используется как признак «вошёл / не вошёл».
 */
class SessionStore(private val context: Context) {

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }

    val profileFlow: Flow<Profile> = context.dataStore.data.map {
        Profile(
            name = it[KEY_NAME].orEmpty(),
            username = it[KEY_USERNAME].orEmpty(),
            uid = it[KEY_UID].orEmpty(),
            server = it[KEY_SERVER].orEmpty(),
            avatarStamp = it[KEY_AVATAR] ?: 0L,
        )
    }

    fun avatarFile(): File = File(context.filesDir, "avatar.jpg")

    suspend fun saveSession(token: String, profile: Profile) {
        avatarFile().delete()
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_NAME] = profile.name
            it[KEY_USERNAME] = profile.username
            it[KEY_UID] = profile.uid
            it[KEY_SERVER] = profile.server
            it[KEY_AVATAR] = 0L
        }
    }

    suspend fun updateProfile(profile: Profile) {
        context.dataStore.edit {
            it[KEY_NAME] = profile.name
            it[KEY_USERNAME] = profile.username
            it[KEY_UID] = profile.uid
            it[KEY_SERVER] = profile.server
            it[KEY_AVATAR] = profile.avatarStamp
        }
    }

    suspend fun clear() {
        avatarFile().delete()
        context.dataStore.edit { it.clear() }
    }
}
