package com.example.storagecleanup.model

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data class Progress(val current: Int, val total: Int, val currentFile: String = "") : ScanState()
    data class Complete(
        val duplicateGroups: List<DuplicateGroup> = emptyList(),
        val trashItems: List<TrashItem> = emptyList(),
        val totalScanned: Int = 0,
        val totalSize: Long = 0,
        val duplicateRecoverable: Long = 0,
        val trashRecoverable: Long = 0
    ) : ScanState()
    data class Error(val message: String) : ScanState()
}
