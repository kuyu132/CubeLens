package com.cubelens

import android.app.Application
import android.util.Log
import com.cubelens.solver.PruningTables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CubeLensApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.IO) {
            try {
                PruningTables.init(filesDir)
                Log.i("CubeLensApp", "Pruning tables ready")
            } catch (e: Exception) {
                Log.e("CubeLensApp", "Failed to init pruning tables", e)
            }
        }
    }
}
