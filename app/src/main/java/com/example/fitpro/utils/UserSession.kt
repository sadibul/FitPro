package com.example.fitpro.utils

import android.content.Context
import android.content.SharedPreferences

class UserSession(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveUserSession(email: String) {
        prefs.edit().apply {
            putString(KEY_CURRENT_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getCurrentUserEmail(): String? {
        return prefs.getString(KEY_CURRENT_USER_EMAIL, null)
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun logout() {
        prefs.edit().apply {
            remove(KEY_CURRENT_USER_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
}
