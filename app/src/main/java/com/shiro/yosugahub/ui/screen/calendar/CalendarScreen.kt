package com.shiro.yosugahub.ui.screen.calendar

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.ui.component.TacticalOutlinedButton
import com.shiro.yosugahub.ui.component.EventRow
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.share.calendarSyncMessage

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val runSync = {
        viewModel.sync { result ->
            Toast.makeText(context, calendarSyncMessage(result), Toast.LENGTH_LONG).show()
        }
    }

    // 権限がまだなら要求し、許可されたらそのまま同期する。
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            runSync()
        } else {
            Toast.makeText(
                context,
                "カレンダーの読み取りが許可されませんでした。端末の設定から許可できます。",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val syncOrRequestPermission = {
        if (viewModel.hasPermission()) {
            runSync()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "今日(${uiState.today})") {
                EventList(uiState.todayEvents)
            }
        }
        item {
            SectionCard(title = "今後7日") {
                EventList(uiState.upcomingEvents)
            }
        }
        item {
            SectionCard(title = "過去7日") {
                EventList(uiState.pastEvents)
            }
        }
        item {
            TacticalOutlinedButton(
                onClick = syncOrRequestPermission,
                enabled = !uiState.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSyncing) "取得中..." else "端末のカレンダーから更新")
            }
        }
    }
}

@Composable
private fun EventList(events: List<com.shiro.yosugahub.domain.model.CalendarEvent>) {
    if (events.isEmpty()) {
        Text(
            text = "予定はありません",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        events.forEach { EventRow(it) }
    }
}
