package com.example.fitpro.utils

import android.content.Context
import android.content.SharedPreferences

class UserSession(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
    
    fun saveUserSession(email: String, rememberMe: Boolean = false) {
        prefs.edit().apply {
            putString(KEY_CURRENT_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }
    
    fun getCurrentUserEmail(): String? {
        return if (isLoggedIn()) {
            prefs.getString(KEY_CURRENT_USER_EMAIL, null)
        } else null
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun shouldRememberUser(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }
    
    fun logout() {
        prefs.edit().apply {
            clear() // Clear all session data completely
            apply()
        }
    }
}
