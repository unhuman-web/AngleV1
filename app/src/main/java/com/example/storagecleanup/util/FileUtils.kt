package com.example.storagecleanup.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import java.security.MessageDigest
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object FileUtils {

    fun hasManageExternalStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun openManageAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun getMediaStoreCollection(mimeType: String): Uri {
        return when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
    }

    fun getMimeTypeForQuery(): Array<String> {
        return arrayOf("image/", "video/", "audio/")
    }

    fun streamFileHash(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getQuarantineDir(context: Context): File {
        return File(context.filesDir, "quarantine").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getQuarantineMetadataFile(context: Context): File {
        return File(context.filesDir, "quarantine_metadata.json")
    }

    fun queryMediaFiles(contentResolver: ContentResolver): List<Uri> {
        val uris = mutableListOf<Uri>()
        val mimePrefixes = getMimeTypeForQuery()

        for (prefix in mimePrefixes) {
            val collection = when (prefix) {
                "image/" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video/" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio/" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> continue
            }

            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("$prefix%")

            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    uris.add(
                        Uri.withAppendedPath(collection, id.toString())
                    )
                }
            }
        }
        return uris
    }
}
