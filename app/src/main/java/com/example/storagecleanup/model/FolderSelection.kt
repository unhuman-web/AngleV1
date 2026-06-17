package com.example.storagecleanup.model

import android.net.Uri

data class FolderSelection(
    val uri: Uri,
    val name: String,
    val path: String
)
