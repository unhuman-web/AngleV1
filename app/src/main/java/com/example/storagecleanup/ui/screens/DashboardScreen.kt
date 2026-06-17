package com.example.storagecleanup.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagecleanup.model.ScanState
import com.example.storagecleanup.ui.components.StatCard
import com.example.storagecleanup.ui.components.StorageInfoRow
import com.example.storagecleanup.viewmodel.CleanupViewModel

@Composable
fun DashboardScreen(
    viewModel: CleanupViewModel,
    onStartScan: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()

    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val totalBytes = stat.totalBytes
    val availableBytes = stat.availableBytes
    val usedBytes = totalBytes - availableBytes

    val completeState = scanState as? ScanState.Complete
    val duplicateRecoverable = completeState?.duplicateRecoverable ?: 0L
    val trashRecoverable = completeState?.trashRecoverable ?: 0L
    val totalRecoverable = duplicateRecoverable + trashRecoverable

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Storage Overview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        StatCard(
            title = "Storage Used",
            value = com.example.storagecleanup.model.FileItem.formatSize(usedBytes),
            subtitle = "of ${com.example.storagecleanup.model.FileItem.formatSize(totalBytes)} total",
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Duplicates",
                value = com.example.storagecleanup.model.FileItem.formatSize(duplicateRecoverable),
                subtitle = "${completeState?.duplicateGroups?.size ?: 0} groups found",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Trash",
                value = com.example.storagecleanup.model.FileItem.formatSize(trashRecoverable),
                subtitle = "${completeState?.trashItems?.size ?: 0} items",
                modifier = Modifier.weight(1f)
            )
        }

        if (totalRecoverable > 0) {
            StatCard(
                title = "Total Recoverable",
                value = com.example.storagecleanup.model.FileItem.formatSize(totalRecoverable),
                subtitle = "Free up space by cleaning duplicates and trash",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Storage Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            StorageInfoRow(label = "Total Space", value = com.example.storagecleanup.model.FileItem.formatSize(totalBytes))
            StorageInfoRow(label = "Used Space", value = com.example.storagecleanup.model.FileItem.formatSize(usedBytes))
            StorageInfoRow(label = "Available Space", value = com.example.storagecleanup.model.FileItem.formatSize(availableBytes))
            if (completeState != null) {
                StorageInfoRow(label = "Files Scanned", value = "${completeState.totalScanned}")
                StorageInfoRow(label = "Duplicate Groups", value = "${completeState.duplicateGroups.size}")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ExtendedFloatingActionButton(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(
                text = if (scanState is ScanState.Scanning) "Scanning…" else "Start Scan",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
