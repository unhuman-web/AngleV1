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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Trash
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagecleanup.model.FileItem
import com.example.storagecleanup.model.TrashItem
import com.example.storagecleanup.ui.components.DeleteConfirmDialog
import com.example.storagecleanup.viewmodel.CleanupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(viewModel: CleanupViewModel) {
    val trashItems by viewModel.trashItems.collectAsState()
    val selectedTrash by viewModel.selectedTrash.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEmptyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTrashItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                actions = {
                    if (trashItems.isNotEmpty()) {
                        TextButton(onClick = {
                            trashItems.forEach { item ->
                                viewModel.toggleTrashSelection(item.file.uri.toString())
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = null)
                            Text("Select All")
                        }
                        TextButton(onClick = { showEmptyDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Text("Empty")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = selectedTrash.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Text(
                        text = "Delete ${selectedTrash.size} files",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Trash,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trash is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Items you delete will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                items(trashItems, key = { it.file.uri.toString() }) { item ->
                    TrashItemCard(
                        item = item,
                        isSelected = item.file.uri.toString() in selectedTrash,
                        onToggle = { viewModel.toggleTrashSelection(item.file.uri.toString()) },
                        onRestore = { viewModel.restoreTrashItem(item) }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val selectedCount = selectedTrash.size
        val selectedSize = trashItems
            .filter { it.file.uri.toString() in selectedTrash }
            .sumOf { it.file.size }

        DeleteConfirmDialog(
            fileCount = selectedCount,
            totalSize = FileItem.formatSize(selectedSize),
            onConfirm = {
                viewModel.deleteSelectedTrashItems()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showEmptyDialog) {
        val totalSize = trashItems.sumOf { it.file.size }
        DeleteConfirmDialog(
            fileCount = trashItems.size,
            totalSize = FileItem.formatSize(totalSize),
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyDialog = false
            },
            onDismiss = { showEmptyDialog = false }
        )
    }
}

@Composable
private fun TrashItemCard(
    item: TrashItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.file.sizeFormatted} • Deleted ${item.formattedDeleteDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.file.isQuarantined) {
                    Text(
                        text = "App Trash",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "System Trash",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private val TrashItem.formattedDeleteDate: String
    get() {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(deletedAt))
    }
