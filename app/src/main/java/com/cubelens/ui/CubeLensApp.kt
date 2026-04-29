package com.cubelens.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cubelens.R
import com.cubelens.data.AppDatabase
import com.cubelens.data.PreferencesManager
import com.cubelens.data.SolveRecord
import com.cubelens.ui.capture.CaptureScreen
import com.cubelens.ui.history.HistoryScreen
import com.cubelens.ui.onboarding.OnboardingScreen
import com.cubelens.ui.review.ReviewScreen
import com.cubelens.ui.solving.SolvingScreen
import com.cubelens.ui.timer.TimerScreen
import com.cubelens.viewmodel.CaptureViewModel
import com.cubelens.viewmodel.SolveViewModel
import kotlinx.coroutines.launch

@Composable
fun CubeLensApp(
  captureViewModel: CaptureViewModel,
  solveViewModel: SolveViewModel,
  context: Context,
) {
  val navController = rememberNavController()
  val prefs = remember { PreferencesManager(context) }
  val scope = rememberCoroutineScope()
  val onboardingCompleted by prefs.onboardingCompleted.collectAsState(initial = null)

  // Wait for onboarding state
  if (onboardingCompleted == null) return

  val startDestination = if (onboardingCompleted == true) BottomTab.SOLVE.route else Routes.ONBOARDING

  val db = remember { AppDatabase.getInstance(context) }
  val solveDao = remember { db.solveDao() }
  val historyRecords by solveDao.getAll().collectAsState(initial = emptyList())

  NavHost(navController = navController, startDestination = startDestination) {
    composable(Routes.ONBOARDING) {
      OnboardingScreen(
        onCompleted = {
          scope.launch { prefs.setOnboardingCompleted(true) }
          navController.navigate(BottomTab.SOLVE.route) {
            popUpTo(Routes.ONBOARDING) { inclusive = true }
          }
        },
      )
    }

    composable(BottomTab.SOLVE.route) {
      MainTabScreen(
        selectedTab = BottomTab.SOLVE,
        onTabSelected = { tab -> navController.navigate(tab.route) { launchSingleTop = true } },
        captureViewModel = captureViewModel,
        solveViewModel = solveViewModel,
        navController = navController,
      )
    }

    composable(BottomTab.TIMER.route) {
      TimerTabScreen(
        selectedTab = BottomTab.TIMER,
        onTabSelected = { tab -> navController.navigate(tab.route) { launchSingleTop = true } },
        onSaveSolve = { scramble, timeMs ->
          scope.launch {
            solveDao.insert(SolveRecord(scramble = scramble, solution = "", moveCount = 0, timeMs = timeMs))
          }
        },
      )
    }

    composable(BottomTab.HISTORY.route) {
      HistoryTabScreen(
        selectedTab = BottomTab.HISTORY,
        onTabSelected = { tab -> navController.navigate(tab.route) { launchSingleTop = true } },
        records = historyRecords,
        onDelete = { record ->
          scope.launch { solveDao.delete(record) }
        },
        onDeleteAll = {
          scope.launch { solveDao.deleteAll() }
        },
      )
    }

    composable(Routes.REVIEW) {
      ReviewScreen(
        viewModel = captureViewModel,
        onBack = { navController.popBackStack() },
        onSolve = { navController.navigate(Routes.SOLVING) },
      )
    }

    composable(Routes.SOLVING) {
      SolvingScreen(
        captureViewModel = captureViewModel,
        solveViewModel = solveViewModel,
        onBack = { navController.popBackStack() },
        onSaveSolve = { scramble, solution, moveCount ->
          scope.launch {
            solveDao.insert(
              SolveRecord(
                scramble = scramble,
                solution = solution,
                moveCount = moveCount,
                timeMs = 0L,
              ),
            )
          }
        },
      )
    }
  }
}

@Composable
private fun MainTabScreen(
  selectedTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
  captureViewModel: CaptureViewModel,
  solveViewModel: SolveViewModel,
  navController: androidx.navigation.NavHostController,
) {
  Scaffold(
    bottomBar = {
      CubeLensNavBar(selectedTab, onTabSelected)
    },
  ) { innerPadding ->
    CaptureScreen(
      viewModel = captureViewModel,
      onReview = { navController.navigate(Routes.REVIEW) },
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
private fun TimerTabScreen(
  selectedTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
  onSaveSolve: (String, Long) -> Unit,
) {
  Scaffold(
    bottomBar = {
      CubeLensNavBar(selectedTab, onTabSelected)
    },
  ) { innerPadding ->
    TimerScreen(
      onSaveSolve = onSaveSolve,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
private fun HistoryTabScreen(
  selectedTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
  records: List<SolveRecord>,
  onDelete: (SolveRecord) -> Unit,
  onDeleteAll: () -> Unit,
) {
  Scaffold(
    bottomBar = {
      CubeLensNavBar(selectedTab, onTabSelected)
    },
  ) { innerPadding ->
    HistoryScreen(
      records = records,
      onDelete = onDelete,
      onDeleteAll = onDeleteAll,
      modifier = Modifier.padding(innerPadding),
    )
  }
}

@Composable
private fun CubeLensNavBar(
  selectedTab: BottomTab,
  onTabSelected: (BottomTab) -> Unit,
) {
  NavigationBar {
    BottomTab.entries.forEach { tab ->
      NavigationBarItem(
        icon = {
          Icon(
            imageVector = ImageVector.vectorResource(tab.iconRes),
            contentDescription = tab.label,
          )
        },
        label = { Text(tab.label) },
        selected = tab == selectedTab,
        onClick = { onTabSelected(tab) },
      )
    }
  }
}

private object Routes {
  const val ONBOARDING = "onboarding"
  const val REVIEW = "review"
  const val SOLVING = "solving"
}

enum class BottomTab(val route: String, val label: String, val iconRes: Int) {
  SOLVE("tab_solve", "Solve", R.drawable.ic_cube),
  TIMER("tab_timer", "Timer", R.drawable.ic_timer),
  HISTORY("tab_history", "History", R.drawable.ic_history),
}
