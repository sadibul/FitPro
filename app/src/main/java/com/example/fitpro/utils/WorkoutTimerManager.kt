package com.example.fitpro.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class WorkoutTimerState(
    val workoutId: Int,
    val totalDuration: Int, // in seconds
    val remainingTime: Int, // in seconds
    val isRunning: Boolean,
    val isPaused: Boolean,
    val startTime: Long, // timestamp when started
    val pausedTime: Long = 0 // total paused time in milliseconds
)

object WorkoutTimerManager {
    private val _activeTimers = MutableStateFlow<Map<Int, WorkoutTimerState>>(emptyMap())
    val activeTimers: StateFlow<Map<Int, WorkoutTimerState>> = _activeTimers
    
    fun startTimer(workoutId: Int, durationMinutes: Int) {
        val currentTime = System.currentTimeMillis()
        val durationSeconds = durationMinutes * 60
        
        val timerState = WorkoutTimerState(
            workoutId = workoutId,
            totalDuration = durationSeconds,
            remainingTime = durationSeconds,
            isRunning = true,
            isPaused = false,
            startTime = currentTime
        )
        
        val currentTimers = _activeTimers.value.toMutableMap()
        currentTimers[workoutId] = timerState
        _activeTimers.value = currentTimers
    }
    
    fun pauseTimer(workoutId: Int) {
        val currentTimers = _activeTimers.value.toMutableMap()
        val timer = currentTimers[workoutId]
        
        if (timer != null && timer.isRunning) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = (currentTime - timer.startTime - timer.pausedTime) / 1000
            val newRemainingTime = (timer.totalDuration - elapsedTime).toInt().coerceAtLeast(0)
            
            currentTimers[workoutId] = timer.copy(
                remainingTime = newRemainingTime,
                isRunning = false,
                isPaused = true
            )
            _activeTimers.value = currentTimers
        }
    }
    
    fun resumeTimer(workoutId: Int) {
        val currentTimers = _activeTimers.value.toMutableMap()
        val timer = currentTimers[workoutId]
        
        if (timer != null && timer.isPaused) {
            val currentTime = System.currentTimeMillis()
            
            currentTimers[workoutId] = timer.copy(
                isRunning = true,
                isPaused = false,
                startTime = currentTime - (timer.totalDuration - timer.remainingTime) * 1000,
                pausedTime = 0
            )
            _activeTimers.value = currentTimers
        }
    }
    
    fun getCurrentRemainingTime(workoutId: Int): Int {
        val timer = _activeTimers.value[workoutId] ?: return 0
        
        if (!timer.isRunning) {
            return timer.remainingTime
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - timer.startTime - timer.pausedTime) / 1000
        val remainingTime = (timer.totalDuration - elapsedTime).toInt()
        
        // Auto-complete when time is up
        if (remainingTime <= 0) {
            val currentTimers = _activeTimers.value.toMutableMap()
            currentTimers[workoutId] = timer.copy(
                remainingTime = 0,
                isRunning = false,
                isPaused = false
            )
            _activeTimers.value = currentTimers
            return 0
        }
        
        return remainingTime
    }
    
    fun removeTimer(workoutId: Int) {
        val currentTimers = _activeTimers.value.toMutableMap()
        currentTimers.remove(workoutId)
        _activeTimers.value = currentTimers
    }
    
    fun isTimerActive(workoutId: Int): Boolean {
        return _activeTimers.value.containsKey(workoutId)
    }
    
    fun isTimerRunning(workoutId: Int): Boolean {
        return _activeTimers.value[workoutId]?.isRunning == true
    }
    
    fun isTimerPaused(workoutId: Int): Boolean {
        return _activeTimers.value[workoutId]?.isPaused == true
    }
    
    fun getActualDurationMinutes(workoutId: Int): Int {
        val timer = _activeTimers.value[workoutId] ?: return 0
        val elapsedSeconds = timer.totalDuration - timer.remainingTime
        return (elapsedSeconds / 60).coerceAtLeast(1) // At least 1 minute
    }
}
