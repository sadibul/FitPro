package com.example.fitpro.utils

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class StepCounterManager(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences("step_counter", Context.MODE_PRIVATE)
    
    // Current user email for user-specific step tracking
    private var currentUserEmail: String? = null
    
    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()
    
    private var isListening = false
    private var initialStepCount = 0
    private var hasInitialCount = false
    
    // Set current user for step tracking
    fun setCurrentUser(userEmail: String?) {
        if (currentUserEmail != userEmail) {
            currentUserEmail = userEmail
            // Reset and load steps for the new user
            hasInitialCount = false
            initialStepCount = 0
            _dailySteps.value = getTodaysSteps()
            resetStepsIfNewDay()
        }
    }
    
    init {
        // Don't reset steps here, wait for user to be set
    }
    
    fun startListening() {
        if (!isListening) {
            stepCounterSensor?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                isListening = true
            }
            stepDetectorSensor?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }
    
    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (!hasInitialCount) {
                        initialStepCount = sensorEvent.values[0].toInt()
                        hasInitialCount = true
                        saveTodaysInitialCount(initialStepCount)
                    } else {
                        val currentTotalSteps = sensorEvent.values[0].toInt()
                        val savedInitialCount = getTodaysInitialCount()
                        val todaysSteps = maxOf(0, currentTotalSteps - savedInitialCount)
                        updateTodaysSteps(todaysSteps)
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    // Each detection represents one step
                    val currentSteps = getTodaysSteps()
                    updateTodaysSteps(currentSteps + 1)
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counting
    }
    
    private fun getTodayString(): String {
        return TimeUtils.getBangladeshDateString()
    }
    
    private fun getUserStepKey(suffix: String): String {
        return if (currentUserEmail != null) {
            "${currentUserEmail}_${suffix}_${getTodayString()}"
        } else {
            "guest_${suffix}_${getTodayString()}"
        }
    }
    
    private fun getTodaysSteps(): Int {
        return if (currentUserEmail != null) {
            preferences.getInt(getUserStepKey("steps"), 0)
        } else {
            0 // No steps for users without login
        }
    }
    
    private fun updateTodaysSteps(steps: Int) {
        if (currentUserEmail == null) return // Don't track steps without user
        
        val validSteps = maxOf(0, steps) // Ensure steps is never negative
        val currentSteps = getTodaysSteps()
        
        // Only update if steps increased or it's the first update
        if (validSteps >= currentSteps || currentSteps == 0) {
            preferences.edit()
                .putInt(getUserStepKey("steps"), validSteps)
                .apply()
            _dailySteps.value = validSteps
        }
    }
    
    private fun getTodaysInitialCount(): Int {
        return if (currentUserEmail != null) {
            preferences.getInt(getUserStepKey("initial_count"), 0)
        } else {
            0
        }
    }
    
    private fun saveTodaysInitialCount(count: Int) {
        if (currentUserEmail == null) return
        
        preferences.edit()
            .putInt(getUserStepKey("initial_count"), count)
            .apply()
    }
    
    private fun resetStepsIfNewDay() {
        if (currentUserEmail == null) return
        
        val lastSavedDate = preferences.getString("${currentUserEmail}_last_date", "")
        val today = getTodayString()
        
        if (lastSavedDate != today) {
            // New day, reset step count for this user
            preferences.edit()
                .putInt(getUserStepKey("steps"), 0)
                .putString("${currentUserEmail}_last_date", today)
                .apply()
            _dailySteps.value = 0
            hasInitialCount = false
        }
    }
    
    fun isStepCounterAvailable(): Boolean {
        return stepCounterSensor != null || stepDetectorSensor != null
    }
    
    // Method to manually add steps for testing
    fun addStepsForTesting(steps: Int) {
        val currentSteps = getTodaysSteps()
        updateTodaysSteps(currentSteps + steps)
    }
    
    // Method to reset steps to 0 (for new targets)
    fun resetSteps() {
        if (currentUserEmail == null) return
        
        val today = getTodayString()
        preferences.edit()
            .putInt(getUserStepKey("steps"), 0)
            .apply()
        _dailySteps.value = 0
        hasInitialCount = false
        initialStepCount = 0
    }
    
    // Method to clear user data on logout
    fun clearUserData() {
        currentUserEmail = null
        _dailySteps.value = 0
        hasInitialCount = false
        initialStepCount = 0
        stopListening()
    }
}
