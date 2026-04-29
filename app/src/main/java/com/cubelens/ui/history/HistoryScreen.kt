package com.cubelens.ui.history

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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

      items(records, key = { it.id }) { record ->
        SolveRecordCard(
          record = record,
          onDelete = { onDelete(record) },
        )
      }

      item { Spacer(Modifier.height(16.dp)) }
    }
  }

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
  onDelete: () -> Unit,
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

  Card(
    modifier = Modifier.fillMaxWidth(),
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
