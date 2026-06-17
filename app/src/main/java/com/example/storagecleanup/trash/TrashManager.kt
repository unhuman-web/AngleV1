package com.example.storagecleanup.trash

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.storagecleanup.model.FileItem
import com.example.storagecleanup.model.TrashItem
import com.example.storagecleanup.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class TrashManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class QuarantineEntry(
        val quarantineId: String,
        val originalPath: String,
        val originalUri: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val deletedAt: Long,
        val originalCollection: String
    )

    suspend fun moveToTrash(file: FileItem): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            moveToSystemTrash(file)
        } else {
            moveToQuarantine(file)
        }
    }

    private fun moveToSystemTrash(file: FileItem): Boolean {
        return try {
            val pendingIntent = MediaStore.createTrashRequest(
                context.contentResolver,
                listOf(file.uri)
            )
            pendingIntent.send()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun moveToQuarantine(file: FileItem): Boolean {
        return try {
            val quarantineDir = FileUtils.getQuarantineDir(context)
            val quarantineId = "q_${System.currentTimeMillis()}_${file.name.hashCode()}"
            val destFile = File(quarantineDir, quarantineId)

            context.contentResolver.openInputStream(file.uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val collection = FileUtils.getMediaStoreCollection(file.mimeType)
            val selection = "${MediaStore.MediaColumns._ID} = ?"
            val selectionArgs = arrayOf(file.uri.lastPathSegment ?: "")

            context.contentResolver.delete(collection, selection, selectionArgs)

            val entry = QuarantineEntry(
                quarantineId = quarantineId,
                originalPath = file.path,
                originalUri = file.uri.toString(),
                fileName = file.name,
                fileSize = file.size,
                mimeType = file.mimeType,
                deletedAt = System.currentTimeMillis(),
                originalCollection = collection.toString()
            )
            saveQuarantineEntry(entry)

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFromTrash(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        if (item.file.isQuarantined && item.quarantineId != null) {
            restoreFromQuarantine(item)
        } else {
            restoreFromSystemTrash(item)
        }
    }

    private fun restoreFromSystemTrash(item: TrashItem): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 0)
            }
            context.contentResolver.update(item.file.uri, values, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun restoreFromQuarantine(item: TrashItem): Boolean {
        return try {
            val quarantineDir = FileUtils.getQuarantineDir(context)
            val quarantineFile = File(quarantineDir, item.quarantineId!!)

            if (!quarantineFile.exists()) return false

            val collection = FileUtils.getMediaStoreCollection(item.file.mimeType)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, item.file.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, getRelativePath(item.file.path))
            }

            val newUri = context.contentResolver.insert(collection, contentValues) ?: return false

            context.contentResolver.openOutputStream(newUri)?.use { output ->
                quarantineFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            quarantineFile.delete()
            removeQuarantineEntry(item.quarantineId)

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun permanentlyDelete(file: FileItem): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteViaMediaStore(file)
        } else {
            deleteDirectly(file)
        }
    }

    private fun deleteViaMediaStore(file: FileItem): Boolean {
        return try {
            val pendingIntent = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(file.uri)
            )
            pendingIntent.send()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDirectly(file: FileItem): Boolean {
        return try {
            val collection = FileUtils.getMediaStoreCollection(file.mimeType)
            context.contentResolver.delete(file.uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteQuarantinedFile(quarantineId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val quarantineDir = FileUtils.getQuarantineDir(context)
            val file = File(quarantineDir, quarantineId)
            val deleted = file.delete()
            if (deleted) removeQuarantineEntry(quarantineId)
            deleted
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getQuarantinedItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        val entries = loadQuarantineEntries()
        val quarantineDir = FileUtils.getQuarantineDir(context)

        entries.mapNotNull { entry ->
            val file = File(quarantineDir, entry.quarantineId)
            if (file.exists()) {
                TrashItem(
                    file = FileItem(
                        uri = Uri.parse(entry.originalUri),
                        name = entry.fileName,
                        path = entry.originalPath,
                        size = entry.fileSize,
                        lastModified = entry.deletedAt,
                        mimeType = entry.mimeType,
                        isQuarantined = true
                    ),
                    originalPath = entry.originalPath,
                    deletedAt = entry.deletedAt,
                    quarantineId = entry.quarantineId
                )
            } else null
        }.sortedByDescending { it.deletedAt }
    }

    suspend fun getSystemTrashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

        val items = mutableListOf<TrashItem>()
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

        val selection = "${MediaStore.MediaColumns.IS_TRASHED} > 0"
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        for (collection in collections) {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(collection, id.toString())

                    items.add(
                        TrashItem(
                            file = FileItem(
                                uri = uri,
                                name = cursor.getString(nameCol) ?: "unknown",
                                path = cursor.getString(dataCol) ?: "",
                                size = cursor.getLong(sizeCol),
                                lastModified = cursor.getLong(dateCol) * 1000,
                                mimeType = cursor.getString(mimeCol) ?: "application/octet-stream",
                                isTrashed = true
                            ),
                            originalPath = cursor.getString(dataCol) ?: "",
                            deletedAt = cursor.getLong(dateCol) * 1000
                        )
                    )
                }
            }
        }
        items
    }

    private fun getRelativePath(fullPath: String): String {
        val dcimIndex = fullPath.indexOf("DCIM")
        val downloadIndex = fullPath.indexOf("Download")
        val musicIndex = fullPath.indexOf("Music")
        val moviesIndex = fullPath.indexOf("Movies")
        val picturesIndex = fullPath.indexOf("Pictures")

        val indices = listOf(dcimIndex, downloadIndex, musicIndex, moviesIndex, picturesIndex)
            .filter { it >= 0 }

        return if (indices.isNotEmpty()) {
            val start = indices.min()
            fullPath.substring(start).substringBeforeLast("/")
        } else {
            "Download"
        }
    }

    private fun saveQuarantineEntry(entry: QuarantineEntry) {
        val entries = loadQuarantineEntries().toMutableList()
        entries.add(entry)
        val file = FileUtils.getQuarantineMetadataFile(context)
        file.writeText(json.encodeToString(entries))
    }

    private fun removeQuarantineEntry(quarantineId: String) {
        val entries = loadQuarantineEntries().toMutableList()
        entries.removeAll { it.quarantineId == quarantineId }
        val file = FileUtils.getQuarantineMetadataFile(context)
        file.writeText(json.encodeToString(entries))
    }

    private fun loadQuarantineEntries(): List<QuarantineEntry> {
        val file = FileUtils.getQuarantineMetadataFile(context)
        return if (file.exists()) {
            try {
                json.decodeFromString(file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
