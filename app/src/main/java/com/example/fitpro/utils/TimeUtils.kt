package com.example.fitpro.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private const val BANGLADESH_TIMEZONE = "Asia/Dhaka"
    
    /**
     * Get current timestamp in Bangladesh timezone
     */
    fun getBangladeshTimeMillis(): Long {
        // timeInMillis always returns UTC, so we just return current time
        // The key is to ensure day boundaries are calculated in Bangladesh timezone
        return System.currentTimeMillis()
    }
    
    /**
     * Get current date string in Bangladesh timezone (yyyy-MM-dd format)
     * Subtract 1 day to fix the off-by-one issue
     */
    fun getBangladeshDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(BANGLADESH_TIMEZONE)
        
        // Subtract 1 day (24 hours in milliseconds) to fix the off-by-one issue
        val adjustedTimestamp = getBangladeshTimeMillis() - (24 * 60 * 60 * 1000)
        
        return dateFormat.format(Date(adjustedTimestamp))
    }
    
    /**
     * Get current date-time string in Bangladesh timezone
     */
    fun getBangladeshDateTimeString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(BANGLADESH_TIMEZONE)
        return dateFormat.format(Date(getBangladeshTimeMillis()))
    }
    
    /**
     * Convert UTC timestamp to Bangladesh local day (returns the day in Bangladesh timezone)
     * Subtract 1 day to fix the off-by-one issue
     */
    fun convertToBangladeshDay(utcTimestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone(BANGLADESH_TIMEZONE)
        
        // Subtract 1 day (24 hours in milliseconds) to fix the off-by-one issue
        val adjustedTimestamp = utcTimestamp - (24 * 60 * 60 * 1000)
        
        return dateFormat.format(Date(adjustedTimestamp))
    }
    
    /**
     * Get start of day in Bangladesh timezone for a given UTC timestamp
     */
    fun getBangladeshDayStart(utcTimestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(BANGLADESH_TIMEZONE))
        calendar.timeInMillis = utcTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Get end of day in Bangladesh timezone for a given UTC timestamp
     */
    fun getBangladeshDayEnd(utcTimestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(BANGLADESH_TIMEZONE))
        calendar.timeInMillis = utcTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Get Bangladesh timezone instance
     */
    fun getBangladeshTimeZone(): TimeZone {
        return TimeZone.getTimeZone(BANGLADESH_TIMEZONE)
    }
    
    /**
     * Parse date string to timestamp in Bangladesh timezone
     */
    fun parseBangladeshDate(dateString: String, format: String = "yyyy-MM-dd"): Long {
        return try {
            val dateFormat = SimpleDateFormat(format, Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone(BANGLADESH_TIMEZONE)
            dateFormat.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
