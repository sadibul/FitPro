package com.example.fitpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedStepTargetDao {
    @Insert
    suspend fun insertCompletedStepTarget(completedStepTarget: CompletedStepTarget)

    @Query("SELECT * FROM completed_step_targets WHERE userEmail = :userEmail ORDER BY completedAt DESC")
    fun getCompletedStepTargets(userEmail: String): Flow<List<CompletedStepTarget>>

    @Query("SELECT * FROM completed_step_targets WHERE userEmail = :userEmail ORDER BY completedAt DESC")
    suspend fun getCompletedStepTargetsForUser(userEmail: String): List<CompletedStepTarget>

    @Query("DELETE FROM completed_step_targets WHERE userEmail = :userEmail")
    suspend fun deleteAllCompletedStepTargets(userEmail: String)
}
