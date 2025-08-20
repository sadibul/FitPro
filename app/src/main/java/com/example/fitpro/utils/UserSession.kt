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
        android.util.Log.d("UserSession", "Saving session - Email: $email, Remember: $rememberMe")
        prefs.edit().apply {
            putString(KEY_CURRENT_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
        android.util.Log.d("UserSession", "Session saved successfully")
    }
    
    fun getCurrentUserEmail(): String? {
        val email = if (isLoggedIn()) {
            prefs.getString(KEY_CURRENT_USER_EMAIL, null)
        } else null
        android.util.Log.d("UserSession", "Getting current email: $email")
        return email
    }
    
    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        android.util.Log.d("UserSession", "Is logged in: $loggedIn")
        return loggedIn
    }
    
    fun shouldRememberUser(): Boolean {
        val remember = prefs.getBoolean(KEY_REMEMBER_ME, false)
        android.util.Log.d("UserSession", "Should remember user: $remember")
        return remember
    }
    
    fun clearSessionIfNotRemembered() {
        // Clear session only if user didn't choose to be remembered
        if (!shouldRememberUser() && isLoggedIn()) {
            logout()
        }
    }
    
    fun logout() {
        prefs.edit().apply {
            clear() // Clear all session data completely
            apply()
        }
    }
}
