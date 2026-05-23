package com.cubelens.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cubelens.R
import com.cubelens.data.SolvePenalty
import com.cubelens.data.SolveRecord
import com.cubelens.ui.timer.formatTime
import com.cubelens.util.aoFromRecords
import com.cubelens.util.bestFromRecords
import com.cubelens.util.formatTime as formatTimeUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  records: List<SolveRecord>,
  onDelete: (SolveRecord) -> Unit,
  onDeleteAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteAllDialog by remember { mutableStateOf(false) }
  var detailRecord by remember { mutableStateOf<SolveRecord?>(null) }
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
        actions = {
          if (records.isNotEmpty()) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TextButton(
                onClick = {
                  val csv = buildExportCsv(records)
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.history_export_chooser))
                    putExtra(Intent.EXTRA_TEXT, csv)
                  }
                  context.startActivity(
                    Intent.createChooser(intent, context.getString(R.string.history_export_chooser)),
                  )
                },
              ) {
                Text(stringResource(R.string.history_export))
              }
              TextButton(onClick = { showDeleteAllDialog = true }) {
                Text(stringResource(R.string.history_clear_all), color = MaterialTheme.colorScheme.error)
              }
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
          .then(modifier)
          .padding(innerPadding)
          .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("🎲", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
          stringResource(R.string.history_empty_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
          stringResource(R.string.history_empty_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
      }
      return@Scaffold
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .then(modifier)
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item { Spacer(Modifier.height(8.dp)) }

      item {
        StatsPanel(records = records)
      }

      items(records, key = { it.id }) { record ->
        SolveRecordCard(
          record = record,
          onOpenDetail = { detailRecord = record },
          onDelete = { onDelete(record) },
        )
      }

      item { Spacer(Modifier.height(16.dp)) }
    }
  }

  if (showDeleteAllDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteAllDialog = false },
      title = { Text(stringResource(R.string.history_clear_title)) },
      text = { Text(stringResource(R.string.history_clear_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteAll()
            showDeleteAllDialog = false
          },
        ) {
          Text(stringResource(R.string.history_clear_confirm), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteAllDialog = false }) {
          Text(stringResource(R.string.history_clear_cancel))
        }
      },
    )
  }

  detailRecord?.let { record ->
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(record.date))
    val movesLine = stringResource(R.string.history_moves_date, record.moveCount, dateStr)

    AlertDialog(
      onDismissRequest = { detailRecord = null },
      title = { Text(stringResource(R.string.history_detail_title)) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(movesLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(stringResource(R.string.history_detail_scramble), style = MaterialTheme.typography.labelMedium)
          Text(
            record.scramble.ifBlank { stringResource(R.string.history_detail_none) },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
          )
          Text(stringResource(R.string.history_detail_solution), style = MaterialTheme.typography.labelMedium)
          Text(
            record.solution.ifBlank { stringResource(R.string.history_detail_none) },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            stringResource(R.string.timer_title) + ": " + recordTimeText(record),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      },
      confirmButton = {
        Row {
          TextButton(
            onClick = {
              val clip = buildString {
                appendLine(recordTimePlain(context, record))
                appendLine(record.scramble)
                if (record.solution.isNotBlank()) appendLine(record.solution)
              }.trim()
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              cm.setPrimaryClip(ClipData.newPlainText("solve", clip))
              Toast.makeText(context, context.getString(R.string.history_detail_copied), Toast.LENGTH_SHORT).show()
            },
          ) {
            Text(stringResource(R.string.history_detail_copy))
          }
          TextButton(
            onClick = {
              val text = buildString {
                appendLine(recordTimePlain(context, record))
                appendLine(record.scramble)
                if (record.solution.isNotBlank()) appendLine(record.solution)
              }
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text.trim())
              }
              context.startActivity(Intent.createChooser(intent, context.getString(R.string.solving_share_chooser)))
            },
          ) {
            Text(stringResource(R.string.history_detail_share))
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { detailRecord = null }) {
          Text(stringResource(R.string.history_detail_close))
        }
      },
    )
  }
}

private fun recordTimePlain(context: Context, record: SolveRecord): String = when (record.penalty) {
  SolvePenalty.DNF -> context.getString(R.string.timer_penalty_dnf)
  SolvePenalty.PLUS2 -> context.getString(R.string.history_time_with_plus2, formatTime(record.timeMs + 2000L))
  else -> formatTime(record.timeMs)
}

@Composable
private fun recordTimeText(record: SolveRecord): String = when (record.penalty) {
  SolvePenalty.DNF -> stringResource(R.string.timer_penalty_dnf)
  SolvePenalty.PLUS2 -> stringResource(R.string.history_time_with_plus2, formatTime(record.timeMs + 2000L))
  else -> formatTime(record.timeMs)
}

@Composable
private fun SolveRecordCard(
  record: SolveRecord,
  onOpenDetail: () -> Unit,
  onDelete: () -> Unit,
) {
  val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
  val dateStr = dateFormat.format(Date(record.date))
  val metaLine = stringResource(R.string.history_moves_date, record.moveCount, dateStr)

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
      Column(
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onOpenDetail),
      ) {
        Text(
          text = recordTimeText(record),
          style = MaterialTheme.typography.titleLarge,
          fontFamily = FontFamily.Monospace,
          color = if (record.penalty == SolvePenalty.DNF) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.primary
          },
        )
        Text(
          text = metaLine,
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
        Text(stringResource(R.string.history_delete), color = MaterialTheme.colorScheme.error)
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
  val bestTime = remember(records) { bestFromRecords(records) }
  val ao5 = remember(records) { aoFromRecords(records, 5) }
  val ao12 = remember(records) { aoFromRecords(records, 12) }
  val ao100 = remember(records) { aoFromRecords(records, 100) }
  val na = stringResource(R.string.history_na)

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
        text = stringResource(R.string.history_stats),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatItem(stringResource(R.string.history_stat_total), "${records.size}")
        Spacer(Modifier.width(8.dp))
        StatItem(stringResource(R.string.history_stat_best), bestTime?.let { formatTimeUtil(it) } ?: na)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatItem(stringResource(R.string.history_stat_ao5), ao5?.let { formatTimeUtil(it) } ?: na)
        Spacer(Modifier.width(8.dp))
        StatItem(stringResource(R.string.history_stat_ao12), ao12?.let { formatTimeUtil(it) } ?: na)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
      ) {
        StatItem(stringResource(R.string.history_stat_ao100), ao100?.let { formatTimeUtil(it) } ?: na)
      }
    }
  }
}

private fun buildExportCsv(records: List<SolveRecord>): String {
  val sb = StringBuilder()
  sb.appendLine("date_iso,time_ms,penalty,move_count,scramble,solution")
  val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }
  fun q(s: String): String = '"' + s.replace("\"", "\"\"").replace("\n", " ") + '"'
  for (r in records.asReversed()) {
    sb.append(fmt.format(Date(r.date))).append(',')
      .append(r.timeMs).append(',')
      .append(r.penalty.ifBlank { SolvePenalty.NONE }).append(',')
      .append(r.moveCount).append(',')
      .append(q(r.scramble)).append(',')
      .append(q(r.solution))
      .appendLine()
  }
  return sb.toString()
}
