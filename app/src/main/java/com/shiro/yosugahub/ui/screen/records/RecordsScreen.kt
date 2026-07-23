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
import androidx.compose.foundation.clickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.documentStatusLabel
import com.shiro.yosugahub.ui.component.itemKindLabel

/** 記録タブの表示区分。 */
private enum class RecordsSection(val label: String) {
    ITEMS("アイテム"),
    DECISIONS("決定"),
    DIARY("日記"),
    DOCUMENTS("文書"),
}

/**
 * 記録タブ(v3-Step 2-d / v4.1 で「文書」を追加)。
 * アイテム(タグ絞込)/ 決定事項ログ / 観察日記 / 未整理文書 を切り替えて表示する。
 */
@Composable
fun RecordsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    var section by rememberSaveable { mutableStateOf(RecordsSection.ITEMS) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showNewItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var documentQuery by rememberSaveable { mutableStateOf("") }
    var documentStatus by rememberSaveable { mutableStateOf<DocumentStatus?>(null) }
    var showNewDocumentDialog by remember { mutableStateOf(false) }
    var openedDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingClassificationOf by remember { mutableStateOf<Document?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "記録", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("検索(タイトル・本文・タグ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showNewItemDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("アイテムを追加")
                    }
                }
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
                val filtered = searchItems(filterItemsByTag(uiState.items, selectedTag), searchQuery)
                if (filtered.isEmpty()) {
                    item {
                        EmptyText(
                            if (uiState.items.isEmpty()) "アイテムはまだありません"
                            else "条件に合うアイテムがありません"
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        ItemCard(item = item, onClick = { editingItem = item })
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

            RecordsSection.DOCUMENTS -> {
                item {
                    OutlinedTextField(
                        value = documentQuery,
                        onValueChange = { documentQuery = it },
                        label = { Text("検索(タイトル・原文・要約・タグ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showNewDocumentDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("文書を追加")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = documentStatus == null,
                            onClick = { documentStatus = null },
                            label = { Text("すべて") },
                        )
                        DocumentStatus.entries.forEach { candidate ->
                            FilterChip(
                                selected = documentStatus == candidate,
                                onClick = {
                                    documentStatus = if (documentStatus == candidate) null else candidate
                                },
                                label = { Text(documentStatusLabel(candidate)) },
                            )
                        }
                    }
                }
                val documents = searchDocuments(
                    filterDocumentsByStatus(uiState.documents, documentStatus),
                    documentQuery,
                )
                if (documents.isEmpty()) {
                    item {
                        EmptyText(
                            if (uiState.documents.isEmpty()) "文書はまだありません"
                            else "条件に合う文書がありません"
                        )
                    }
                } else {
                    items(documents, key = { it.id }) { document ->
                        DocumentCard(
                            document = document,
                            onClick = { openedDocumentId = document.id },
                        )
                    }
                }
            }
        }
    }

    if (showNewItemDialog) {
        ItemEditDialog(
            original = null,
            onDismiss = { showNewItemDialog = false },
            onSave = { kind, title, body, tags ->
                viewModel.addItem(kind, title, body, tags)
                showNewItemDialog = false
            },
        )
    }

    editingItem?.let { item ->
        ItemEditDialog(
            original = item,
            onDismiss = { editingItem = null },
            onSave = { kind, title, body, tags ->
                viewModel.updateItem(item.copy(kind = kind, title = title, body = body, tags = tags))
                editingItem = null
            },
            onDelete = {
                viewModel.deleteItem(item.id)
                editingItem = null
            },
        )
    }

    if (showNewDocumentDialog) {
        DocumentAddDialog(
            onDismiss = { showNewDocumentDialog = false },
            onSave = { title, body ->
                viewModel.addDocument(title, body)
                showNewDocumentDialog = false
            },
        )
    }

    // 状態が更新されたら開いている詳細も追従するよう、IDで引き直す。
    val openedDocument = openedDocumentId?.let { id -> uiState.documents.firstOrNull { it.id == id } }
    if (openedDocumentId != null && openedDocument == null) {
        openedDocumentId = null  // 削除された文書のダイアログは閉じる
    }
    openedDocument?.let { document ->
        DocumentDetailDialog(
            document = document,
            onDismiss = { openedDocumentId = null },  // 保留 = 状態を動かさず閉じる
            onDelete = {
                viewModel.deleteDocument(document.id)
                openedDocumentId = null
            },
            onApprove = {
                viewModel.approveDocument(document.id)
                openedDocumentId = null
            },
            onEdit = { editingClassificationOf = document },
            onReclassify = {
                viewModel.reclassifyDocument(document.id)
                openedDocumentId = null
            },
            onArchive = {
                viewModel.archiveDocument(document.id)
                openedDocumentId = null
            },
        )
    }

    editingClassificationOf?.let { document ->
        ClassificationEditDialog(
            original = document.currentClassification,
            onDismiss = { editingClassificationOf = null },
            onApprove = { edits ->
                viewModel.approveDocumentWithEdits(document.id, edits)
                editingClassificationOf = null
                openedDocumentId = null
            },
        )
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
private fun ItemCard(item: KnowledgeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(title = item.title, modifier = modifier.clickable(onClick = onClick)) {
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
private fun DocumentCard(document: Document, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(title = document.title, modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = document.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(onClick = {}, label = { Text(documentStatusLabel(document.status)) })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = documentPreviewOf(document), style = MaterialTheme.typography.bodyMedium)
            val tags = document.currentClassification?.tags.orEmpty()
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiaryCard(entry: DiaryEntry, modifier: Modifier = Modifier) {
    SectionCard(title = entry.date, modifier = modifier) {
        Text(text = entry.body, style = MaterialTheme.typography.bodyMedium)
    }
}
