package com.shiro.yosugahub.ui.screen.records

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.itemKindLabel

/** 記録タブの表示区分。 */
private enum class RecordsSection(val label: String) {
    ITEMS("アイテム"),
    DECISIONS("決定"),
    DIARY("日記"),
}

/**
 * 記録タブ(v3-Step 2-d)。知識ベースの閲覧専用UI。
 * アイテム(タグ絞込)/ 決定事項ログ / 観察日記 を切り替えて表示する。
 */
@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    var section by rememberSaveable { mutableStateOf(RecordsSection.ITEMS) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "記録", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordsSection.entries.forEach { candidate ->
                    FilterChip(
                        selected = section == candidate,
                        onClick = { section = candidate },
                        label = { Text(candidate.label) },
                    )
                }
            }
        }

        when (section) {
            RecordsSection.ITEMS -> {
                if (uiState.tagNames.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("すべて") },
                            )
                            uiState.tagNames.forEach { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = {
                                        selectedTag = if (selectedTag == tag) null else tag
                                    },
                                    label = { Text("#$tag") },
                                )
                            }
                        }
                    }
                }
                val filtered = filterItemsByTag(uiState.items, selectedTag)
                if (filtered.isEmpty()) {
                    item { EmptyText("アイテムはまだありません") }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        ItemCard(item)
                    }
                }
            }

            RecordsSection.DECISIONS -> {
                val decisions = decisionsOf(uiState.items)
                if (decisions.isEmpty()) {
                    item { EmptyText("決定事項はまだありません") }
                } else {
                    items(decisions, key = { it.id }) { item ->
                        DecisionCard(item)
                    }
                }
            }

            RecordsSection.DIARY -> {
                if (uiState.diaryEntries.isEmpty()) {
                    item { EmptyText("観察日記はまだありません") }
                } else {
                    items(uiState.diaryEntries, key = { it.id }) { entry ->
                        DiaryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ItemCard(item: KnowledgeItem, modifier: Modifier = Modifier) {
    SectionCard(title = item.title, modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(onClick = {}, label = { Text(itemKindLabel(item.kind)) })
            }
            if (item.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.body, style = MaterialTheme.typography.bodyMedium)
            }
            val meta = buildList {
                if (item.tags.isNotEmpty()) add(item.tags.joinToString(" ") { "#$it" })
                if (item.entities.isNotEmpty()) {
                    add(item.entities.joinToString(" / ", prefix = "関連: ") { it.name })
                }
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meta.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DecisionCard(item: KnowledgeItem, modifier: Modifier = Modifier) {
    SectionCard(title = "${item.createdAt.take(10)}  ${item.title}", modifier = modifier) {
        if (item.body.isNotBlank()) {
            Text(text = item.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DiaryCard(entry: DiaryEntry, modifier: Modifier = Modifier) {
    SectionCard(title = entry.date, modifier = modifier) {
        Text(text = entry.body, style = MaterialTheme.typography.bodyMedium)
    }
}
