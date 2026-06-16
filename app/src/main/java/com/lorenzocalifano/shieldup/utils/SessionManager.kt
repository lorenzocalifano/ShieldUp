package com.lorenzocalifano.shieldup.utils

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences("shield_session", Context.MODE_PRIVATE)

    fun saveSession(userId: String, name: String, email: String, role: String) {
        preferences.edit()
            .putString("userId", userId)
            .putString("name", name)
            .putString("email", email)
            .putString("role", role)
            .putBoolean("logged", true)
            .apply()
    }

    fun isLogged(): Boolean {
        return preferences.getBoolean("logged", false)
    }

    fun getUserId(): String {
        return preferences.getString("userId", "") ?: ""
    }

    fun getName(): String {
        return preferences.getString("name", "") ?: ""
    }

    fun getEmail(): String {
        return preferences.getString("email", "") ?: ""
    }

    fun getRole(): String {
        return preferences.getString("role", "STANDARD") ?: "STANDARD"
    }

    fun clearSession() {
        preferences.edit().clear().apply()
    }
}