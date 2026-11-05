package com.example.liftlogic.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembers who is logged in, so the user stays signed in after closing the app. */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("liftlogic_session", Context.MODE_PRIVATE)

    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    val currentUserId: String? get() = _userId.value

    fun login(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        _userId.value = userId
    }

    fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _userId.value = null
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
    }
}
