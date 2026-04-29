package com.cubelens.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solve_history")
data class SolveRecord(
  val scramble: String,
  val solution: String,
  val moveCount: Int,
  val timeMs: Long,
  val date: Long = System.currentTimeMillis(),
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
