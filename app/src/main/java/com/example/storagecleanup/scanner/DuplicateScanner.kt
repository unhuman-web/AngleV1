package com.example.storagecleanup.scanner

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.storagecleanup.model.DuplicateGroup
import com.example.storagecleanup.model.FileItem
import com.example.storagecleanup.model.ScanState
import com.example.storagecleanup.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class DuplicateScanner(private val context: Context) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    suspend fun scan(uris: List<Uri> = emptyList()) = withContext(Dispatchers.IO) {
        try {
            _scanState.value = ScanState.Scanning

            val contentResolver = context.contentResolver
            val filesToScan = if (uris.isNotEmpty()) {
                uris.mapNotNull { uri -> getFileItemFromUri(contentResolver, uri) }
            } else {
                scanAllMediaFiles(contentResolver)
            }

            val totalFiles = filesToScan.size
            _scanState.value = ScanState.Progress(0, totalFiles)

            val sizeGroups = mutableMapOf<Long, MutableList<FileItem>>()
            filesToScan.forEachIndexed { index, fileItem ->
                ensureActive()
                _scanState.value = ScanState.Progress(index + 1, totalFiles, fileItem.name)

                sizeGroups.getOrPut(fileItem.size) { mutableListOf() }.add(fileItem)
            }

            val potentialDuplicates = sizeGroups.filter { it.value.size > 1 }
            val hashGroups = mutableMapOf<String, MutableList<FileItem>>()

            var hashed = 0
            val totalToHash = potentialDuplicates.values.sumOf { it.size }

            for ((_, filesOfSize) in potentialDuplicates) {
                for (fileItem in filesOfSize) {
                    ensureActive()
                    hashed++
                    _scanState.value = ScanState.Progress(
                        totalFiles + hashed,
                        totalFiles + totalToHash,
                        "Hashing: ${fileItem.name}"
                    )

                    val hash = hashFile(fileItem.uri)
                    if (hash != null) {
                        hashGroups.getOrPut(hash) { mutableListOf() }.add(fileItem)
                    }
                }
            }

            val duplicateGroups = hashGroups
                .filter { it.value.size > 1 }
                .map { (hash, files) -> DuplicateGroup(hash = hash, files = files.sortedBy { it.name }) }
                .sortedByDescending { it.recoverableSize }

            val totalSize = filesToScan.sumOf { it.size }
            val duplicateRecoverable = duplicateGroups.sumOf { it.recoverableSize }

            _scanState.value = ScanState.Complete(
                duplicateGroups = duplicateGroups,
                totalScanned = totalFiles,
                totalSize = totalSize,
                duplicateRecoverable = duplicateRecoverable
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            _scanState.value = ScanState.Idle
            throw e
        } catch (e: Exception) {
            _scanState.value = ScanState.Error(e.message ?: "Unknown error during scan")
        }
    }

    private fun scanAllMediaFiles(contentResolver: ContentResolver): List<FileItem> {
        val files = mutableListOf<FileItem>()

        val collections = arrayOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )

        for (collection in collections) {
            contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "unknown"
                    val path = cursor.getString(dataCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"

                    if (size > 0) {
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        files.add(
                            FileItem(
                                uri = uri,
                                name = name,
                                path = path,
                                size = size,
                                lastModified = date,
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        }
        return files
    }

    private fun getFileItemFromUri(contentResolver: ContentResolver, uri: Uri): FileItem? {
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.MIME_TYPE
            )

            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                    FileItem(
                        uri = uri,
                        name = cursor.getString(nameCol) ?: "unknown",
                        path = cursor.getString(dataCol) ?: "",
                        size = cursor.getLong(sizeCol),
                        lastModified = cursor.getLong(dateCol) * 1000,
                        mimeType = cursor.getString(mimeCol) ?: "application/octet-stream"
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun hashFile(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                FileUtils.streamFileHash(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
