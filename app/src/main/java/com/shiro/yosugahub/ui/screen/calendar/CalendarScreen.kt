package com.shiro.yosugahub.ui.screen.calendar

import android.widget.Toast
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
import com.shiro.yosugahub.ui.component.EventRow
import com.shiro.yosugahub.ui.component.SectionCard

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "カレンダー", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard(title = "今日") {
                uiState.todayEvents.forEach { EventRow(it) }
            }
        }
        item {
            SectionCard(title = "今後7日") {
                uiState.upcomingEvents.forEach { EventRow(it) }
            }
        }
        item {
            SectionCard(title = "過去7日") {
                uiState.pastEvents.forEach { EventRow(it) }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Phase 4 で実装予定です", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("手動更新")
            }
        }
    }
}
