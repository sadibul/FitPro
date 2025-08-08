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
    
    private val _dailySteps = MutableStateFlow(getTodaysSteps())
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()
    
    private var isListening = false
    private var initialStepCount = 0
    private var hasInitialCount = false
    
    init {
        // Reset steps if it's a new day
        resetStepsIfNewDay()
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
                        val todaysSteps = currentTotalSteps - savedInitialCount
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
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    private fun getTodaysSteps(): Int {
        return preferences.getInt("steps_${getTodayString()}", 0)
    }
    
    private fun updateTodaysSteps(steps: Int) {
        preferences.edit()
            .putInt("steps_${getTodayString()}", steps)
            .apply()
        _dailySteps.value = steps
    }
    
    private fun getTodaysInitialCount(): Int {
        return preferences.getInt("initial_count_${getTodayString()}", 0)
    }
    
    private fun saveTodaysInitialCount(count: Int) {
        preferences.edit()
            .putInt("initial_count_${getTodayString()}", count)
            .apply()
    }
    
    private fun resetStepsIfNewDay() {
        val lastSavedDate = preferences.getString("last_date", "")
        val today = getTodayString()
        
        if (lastSavedDate != today) {
            // New day, reset step count
            preferences.edit()
                .putInt("steps_$today", 0)
                .putString("last_date", today)
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
}
