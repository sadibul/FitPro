package com.example.fitpro

import android.app.Application
import androidx.room.Room
import com.example.fitpro.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FitProApplication : Application() {
    
    // Application-wide coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        // Enable strict mode for debug builds to catch performance issues
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        
        // Initialize database
        database = AppDatabase.getDatabase(this)
    }
    
    private fun enableStrictMode() {
        try {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        } catch (e: Exception) {
            // Ignore strict mode errors in case of issues
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Clean up database connections
        AppDatabase.closeDatabase()
    }
}
