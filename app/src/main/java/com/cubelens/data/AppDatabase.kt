package com.cubelens.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SolveRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun solveDao(): SolveDao

  companion object {
    @Volatile
    private var instance: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE solve_history ADD COLUMN penalty TEXT NOT NULL DEFAULT ''")
      }
    }

    fun getInstance(context: Context): AppDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "cubelens.db",
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration()
          .build().also { instance = it }
      }
  }
}
