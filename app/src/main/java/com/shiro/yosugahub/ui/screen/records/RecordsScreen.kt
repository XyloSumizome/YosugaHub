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
import com.shiro.yosugahub.ui.component.TacticalOutlinedButton
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.Document
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.EntityType
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.ui.component.SectionCard
import com.shiro.yosugahub.ui.component.StatusTag
import com.shiro.yosugahub.ui.component.directiveStatusLabel
import com.shiro.yosugahub.ui.component.documentStatusLabel
import com.shiro.yosugahub.ui.component.entityTypeLabel
import com.shiro.yosugahub.ui.component.itemKindLabel

/** 記録タブの表示区分。 */
private enum class RecordsSection(val label: String) {
    ITEMS("アイテム"),
    DECISIONS("決定"),
    DIARY("日記"),
    DOCUMENTS("文書"),
    ENTITIES("関連"),
    DIRECTIVES("指示"),
}

/**
 * 記録タブ(v3-Step 2-d / v4.1「文書」/ その後「関連」/ v4.2「指示」を追加)。
 * アイテム(タグ絞込)/ 決定事項ログ / 観察日記 / 未整理文書 / 実体 / 指示書 を切り替えて表示する。
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
    var entityType by rememberSaveable { mutableStateOf<EntityType?>(null) }
    var directiveProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedDirectiveId by rememberSaveable { mutableStateOf<String?>(null) }

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
                    TacticalOutlinedButton(
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
                    TacticalOutlinedButton(
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

            RecordsSection.ENTITIES -> {
                val index = buildEntityIndex(uiState.entities, uiState.items)
                val types = entityTypesPresent(index)
                if (types.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = entityType == null,
                                onClick = { entityType = null },
                                label = { Text("すべて") },
                            )
                            types.forEach { candidate ->
                                FilterChip(
                                    selected = entityType == candidate,
                                    onClick = {
                                        entityType = if (entityType == candidate) null else candidate
                                    },
                                    label = { Text(entityTypeLabel(candidate)) },
                                )
                            }
                        }
                    }
                }
                val filtered = filterEntitiesByType(index, entityType)
                if (filtered.isEmpty()) {
                    item {
                        EmptyText(
                            if (index.isEmpty())
                                "関連はまだありません。ヨスガが会話から関連付けると増えていきます。"
                            else "条件に合う関連がありません"
                        )
                    }
                } else {
                    items(filtered, key = { it.entity.id }) { entry ->
                        EntityCard(entry)
                    }
                }
            }

            RecordsSection.DIRECTIVES -> {
                val targets = directiveTargetsPresent(uiState.directives, uiState.projects)
                if (targets.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = directiveProjectId == null,
                                onClick = { directiveProjectId = null },
                                label = { Text("すべて") },
                            )
                            targets.forEach { project ->
                                FilterChip(
                                    selected = directiveProjectId == project.id,
                                    onClick = {
                                        directiveProjectId =
                                            if (directiveProjectId == project.id) null else project.id
                                    },
                                    label = { Text(project.name) },
                                )
                            }
                        }
                    }
                }
                val directives = sortDirectivesForDisplay(
                    filterDirectivesByProject(uiState.directives, directiveProjectId)
                )
                if (directives.isEmpty()) {
                    item {
                        EmptyText(
                            if (uiState.directives.isEmpty())
                                "指示書はまだありません。ヨスガの提案を承認すると、ここから配信されます。"
                            else "条件に合う指示書がありません"
                        )
                    }
                } else {
                    items(directives, key = { it.id }) { directive ->
                        DirectiveCard(
                            directive = directive,
                            targetName = directiveTargetName(directive, uiState.projects),
                            onClick = { openedDirectiveId = directive.id },
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
    // 対象が消えていれば let が空振りしてダイアログは閉じる(composition 中に状態は書き換えない)。
    val openedDocument = openedDocumentId?.let { id -> uiState.documents.firstOrNull { it.id == id } }
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

    // 対象が消えていれば let が空振りしてダイアログは閉じる。
    val openedDirective = openedDirectiveId?.let { id ->
        uiState.directives.firstOrNull { it.id == id }
    }
    openedDirective?.let { directive ->
        DirectiveDetailDialog(
            directive = directive,
            targetName = directiveTargetName(directive, uiState.projects),
            onDismiss = { openedDirectiveId = null },
            onMarkDone = {
                viewModel.markDirectiveDone(directive.id)
                openedDirectiveId = null
            },
            onReopen = {
                viewModel.reopenDirective(directive.id)
                openedDirectiveId = null
            },
            onDelete = {
                viewModel.deleteDirective(directive.id)
                openedDirectiveId = null
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
                StatusTag(itemKindLabel(item.kind))
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
                StatusTag(documentStatusLabel(document.status))
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
private fun DirectiveCard(
    directive: Directive,
    targetName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = directive.title.ifBlank { "$targetName への指示" },
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$targetName / ${directive.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusTag(directiveStatusLabel(directive.status))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = directive.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 実体1件と、その実体に関連付けられたアイテムの見出し。 */
@Composable
private fun EntityCard(entry: EntityIndex, modifier: Modifier = Modifier) {
    SectionCard(title = entry.entity.name, modifier = modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "関連 ${entry.items.size} 件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusTag(entityTypeLabel(entry.entity.type))
            }
            if (entry.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.items.joinToString("\n") { "・${it.title}" },
                    style = MaterialTheme.typography.bodyMedium,
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
