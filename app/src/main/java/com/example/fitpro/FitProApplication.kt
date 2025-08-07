package com.example.fitpro

import android.app.Application
import com.example.fitpro.data.AppDatabase

class FitProApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
}
