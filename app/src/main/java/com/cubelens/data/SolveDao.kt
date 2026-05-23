package com.cubelens.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolveDao {
  @Query("SELECT * FROM solve_history ORDER BY date DESC")
  fun getAll(): Flow<List<SolveRecord>>

  @Insert
  suspend fun insert(record: SolveRecord): Long

  @Delete
  suspend fun delete(record: SolveRecord)

  @Query("DELETE FROM solve_history")
  suspend fun deleteAll()

  @Query("SELECT * FROM solve_history ORDER BY date DESC LIMIT :limit")
  fun getRecent(limit: Int): Flow<List<SolveRecord>>

  @Query("SELECT MIN(timeMs) FROM solve_history")
  fun getBestTime(): Flow<Long?>
}
