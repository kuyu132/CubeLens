package com.cubelens.ui.history

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cubelens.data.SolveRecord
import com.cubelens.ui.timer.formatTime
import com.cubelens.util.calculateAoN
import com.cubelens.util.formatTime as formatTimeUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  records: List<SolveRecord>,
  onDelete: (SolveRecord) -> Unit,
  onDeleteAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteAllDialog by remember { mutableStateOf(false) }
  var selectedRecord by remember { mutableStateOf<SolveRecord?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var filterType by remember { mutableStateOf(FilterType.ALL) }
  var sortNewestFirst by remember { mutableStateOf(true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("History") },
        actions = {
          if (records.isNotEmpty()) {
            TextButton(onClick = { showDeleteAllDialog = true }) {
              Text("Clear All", color = MaterialTheme.colorScheme.error)
            }
          }
        },
      )
    },
  ) { innerPadding ->
    if (records.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("🎲", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
          "No solves yet",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
          "Use the timer or solve a cube to see history here",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
      }
      return@Scaffold
    }

    // Apply filter and sort
    val filteredRecords = remember(records, filterType, sortNewestFirst) {
      val filtered = when (filterType) {
        FilterType.ALL -> records
        FilterType.TIMED -> records.filter { it.moveCount == 0 || it.solution.isBlank() }
        FilterType.SOLVED -> records.filter { it.moveCount > 0 && it.solution.isNotBlank() }
      }
      if (sortNewestFirst) filtered.sortedByDescending { it.date }
      else filtered.sortedBy { it.timeMs }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item { Spacer(Modifier.height(8.dp)) }

      // Stats panel
      item {
        StatsPanel(records = records)
      }

      // Filter + Sort row
      item {
        FilterSortRow(
          filterType = filterType,
          onFilterChange = { filterType = it },
          sortNewestFirst = sortNewestFirst,
          onSortToggle = { sortNewestFirst = !sortNewestFirst },
        )
      }

      if (filteredRecords.isEmpty()) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("No records match this filter", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      } else {
        items(filteredRecords, key = { it.id }) { record ->
          SolveRecordCard(
            record = record,
            onClick = { selectedRecord = record },
            onDelete = {
              selectedRecord = record
              showDeleteConfirm = true
            },
          )
        }
      }

      item { Spacer(Modifier.height(16.dp)) }
    }
  }

  // Detail bottom sheet
  if (selectedRecord != null && !showDeleteConfirm) {
    ModalBottomSheet(
      onDismissRequest = { selectedRecord = null },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      HistoryDetailSheet(
        record = selectedRecord!!,
        onDismiss = { selectedRecord = null },
        onDelete = {
          onDelete(selectedRecord!!)
          selectedRecord = null
        },
      )
    }
  }

  // Individual delete confirmation
  if (showDeleteConfirm && selectedRecord != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false; selectedRecord = null },
      title = { Text("Delete this record?") },
      text = {
        Column {
          Text("This will permanently delete:")
          selectedRecord?.let {
            Spacer(Modifier.height(8.dp))
            Text(
              text = formatTime(it.timeMs),
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            )
            if (it.moveCount > 0) {
              Text("${it.moveCount} moves")
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            selectedRecord?.let { onDelete(it) }
            showDeleteConfirm = false
            selectedRecord = null
          },
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false; selectedRecord = null }) {
          Text("Cancel")
        }
      },
    )
  }

  // Delete All confirmation
  if (showDeleteAllDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteAllDialog = false },
      title = { Text("Clear All History?") },
      text = { Text("This will permanently delete all solve records.") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteAll()
            showDeleteAllDialog = false
          },
        ) {
          Text("Delete All", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteAllDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun SolveRecordCard(
  record: SolveRecord,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = formatTime(record.timeMs),
          style = MaterialTheme.typography.titleLarge,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "${record.moveCount} moves · ${dateFormat.format(Date(record.date))}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (record.scramble.isNotBlank()) {
          Text(
            text = record.scramble,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 2,
          )
        }
      }
      OutlinedButton(onClick = onDelete) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
      }
    }
  }
}

@Composable
private fun HistoryDetailSheet(
  record: SolveRecord,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
) {
  val context = LocalContext.current
  val dateFormat = remember { SimpleDateFormat("MMM dd yyyy, HH:mm:ss", Locale.getDefault()) }
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .padding(bottom = 32.dp)
      .verticalScroll(scrollState),
  ) {
    // Time header
    Text(
      text = formatTime(record.timeMs),
      style = MaterialTheme.typography.displaySmall,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = dateFormat.format(Date(record.date)),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))

    // Scramble section
    if (record.scramble.isNotBlank()) {
      DetailSection(title = "Scramble", content = record.scramble)
      Spacer(Modifier.height(16.dp))
    }

    // Solution section
    if (record.solution.isNotBlank()) {
      DetailSection(title = "Solution (${record.moveCount} moves)", content = record.solution)
      Spacer(Modifier.height(16.dp))
    }

    // Stats
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      DetailStat(label = "Time", value = formatTime(record.timeMs))
      DetailStat(label = "Moves", value = "${record.moveCount}")
      DetailStat(
        label = "Type",
        value = if (record.moveCount > 0 && record.solution.isNotBlank()) "Solved" else "Timed",
      )
    }

    Spacer(Modifier.height(24.dp))

    // Action buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
        onClick = {
          val shareText = buildString {
            appendLine("🧊 CubeLens")
            if (record.scramble.isNotBlank()) appendLine("Scramble: ${record.scramble}")
            if (record.solution.isNotBlank()) appendLine("Solution: ${record.solution} (${record.moveCount} moves)")
            appendLine("Time: ${formatTime(record.timeMs)}")
          }.trimEnd()
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
          }
          context.startActivity(Intent.createChooser(intent, "Share"))
        },
        modifier = Modifier.weight(1f),
      ) {
        Text("Share")
      }
      OutlinedButton(
        onClick = onDelete,
        modifier = Modifier.weight(1f),
      ) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
      }
    }
  }
}

@Composable
private fun DetailSection(title: String, content: String) {
  Column {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = content,
      style = MaterialTheme.typography.bodyMedium,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun DetailStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun StatItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun StatsPanel(records: List<SolveRecord>) {
  val times = remember(records) { records.map { it.timeMs } }
  val bestTime = remember(times) { times.minOrNull() }
  val ao5 = remember(times) { calculateAoN(times, 5) }
  val ao12 = remember(times) { calculateAoN(times, 12) }
  val ao100 = remember(times) { calculateAoN(times, 100) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "📊 Statistics",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Row 1: Total and Best
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatItem("Total", "${records.size}")
        Spacer(Modifier.width(8.dp))
        StatItem("Best", bestTime?.let { formatTimeUtil(it) } ?: "N/A")
      }

      // Row 2: Ao5 and Ao12
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatItem("Ao5", ao5?.let { formatTimeUtil(it) } ?: "N/A")
        Spacer(Modifier.width(8.dp))
        StatItem("Ao12", ao12?.let { formatTimeUtil(it) } ?: "N/A")
      }

      // Row 3: Ao100
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
      ) {
        StatItem("Ao100", ao100?.let { formatTimeUtil(it) } ?: "N/A")
      }
    }
  }
}

// ── Filter & Sort ────────────────────────────────────────────────────────────

private enum class FilterType { ALL, TIMED, SOLVED }

@Composable
private fun FilterSortRow(
  filterType: FilterType,
  onFilterChange: (FilterType) -> Unit,
  sortNewestFirst: Boolean,
  onSortToggle: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Filter chips
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      FilterType.entries.forEach { type ->
        val selected = type == filterType
        TextButton(
          onClick = { onFilterChange(type) },
        ) {
          Text(
            text = when (type) {
              FilterType.ALL -> "All"
              FilterType.TIMED -> "Timed"
              FilterType.SOLVED -> "Solved"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }

    // Sort toggle
    TextButton(onClick = onSortToggle) {
      Text(
        text = if (sortNewestFirst) "↓ Newest" else "↓ Fastest",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
