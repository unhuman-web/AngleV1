package com.example.storagecleanup.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagecleanup.model.DuplicateGroup
import com.example.storagecleanup.model.FileItem
import com.example.storagecleanup.model.ScanState
import com.example.storagecleanup.ui.components.DeleteConfirmDialog
import com.example.storagecleanup.ui.components.FileItemCard
import com.example.storagecleanup.viewmodel.CleanupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(viewModel: CleanupViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    val selectedDuplicates by viewModel.selectedDuplicates.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val completeState = scanState as? ScanState.Complete
    val duplicateGroups = completeState?.duplicateGroups ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                actions = {
                    if (duplicateGroups.isNotEmpty()) {
                        TextButton(onClick = {
                            duplicateGroups.forEach { group ->
                                viewModel.selectAllDuplicatesInGroup(group)
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = null)
                            Text("Select All")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = selectedDuplicates.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Text(
                        text = "Delete ${selectedDuplicates.size} files",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (duplicateGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No duplicates found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(duplicateGroups, key = { it.hash }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedKeys = selectedDuplicates,
                        onToggle = { fileKey -> viewModel.toggleDuplicateSelection(fileKey) },
                        onSelectAll = { viewModel.selectAllDuplicatesInGroup(group) }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val selectedCount = selectedDuplicates.size
        val selectedSize = duplicateGroups.flatMap { it.files.drop(1) }
            .filter { "${it.uri}" in selectedDuplicates }
            .sumOf { it.size }

        DeleteConfirmDialog(
            fileCount = selectedCount,
            totalSize = FileItem.formatSize(selectedSize),
            onConfirm = {
                viewModel.deleteSelectedDuplicates(duplicateGroups)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedKeys: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${group.count} copies • ${FileItem.formatSize(group.totalSize)} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Recoverable: ${FileItem.formatSize(group.recoverableSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onSelectAll) {
                    Text("Select Duplicates")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            group.files.forEach { file ->
                val fileKey = "${group.hash}_${file.uri}"
                val isSelected = fileKey in selectedKeys
                FileItemCard(
                    file = file,
                    isSelected = isSelected,
                    onToggle = { onToggle(fileKey) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
