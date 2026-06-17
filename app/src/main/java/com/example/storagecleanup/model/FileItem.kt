package com.example.storagecleanup.model

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val isTrashed: Boolean = false,
    val isQuarantined: Boolean = false
) {
    val sizeFormatted: String
        get() = formatSize(size)

    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")

    companion object {
        fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}

data class DuplicateGroup(
    val hash: String,
    val files: List<FileItem>
) {
    val totalSize: Long get() = files.sumOf { it.size }
    val recoverableSize: Long get() = totalSize - (files.minOfOrNull { it.size } ?: 0)
    val count: Int get() = files.size
    val displayName: String get() = files.firstOrNull()?.name ?: "Unknown"
}

data class TrashItem(
    val file: FileItem,
    val originalPath: String,
    val deletedAt: Long,
    val quarantineId: String? = null
)
