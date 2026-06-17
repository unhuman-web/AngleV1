package com.example.storagecleanup.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagecleanup.model.DuplicateGroup
import com.example.storagecleanup.model.ScanState
import com.example.storagecleanup.model.TrashItem
import com.example.storagecleanup.scanner.DuplicateScanner
import com.example.storagecleanup.trash.TrashManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CleanupViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = DuplicateScanner(application)
    private val trashManager = TrashManager(application)

    val scanState: StateFlow<ScanState> = scanner.scanState

    private val _trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    val trashItems: StateFlow<List<TrashItem>> = _trashItems.asStateFlow()

    private val _selectedDuplicates = MutableStateFlow<Set<String>>(emptySet())
    val selectedDuplicates: StateFlow<Set<String>> = _selectedDuplicates.asStateFlow()

    private val _selectedTrash = MutableStateFlow<Set<String>>(emptySet())
    val selectedTrash: StateFlow<Set<String>> = _selectedTrash.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    fun startScan(uris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            scanner.scan(uris)
        }
    }

    fun loadTrashItems() {
        viewModelScope.launch {
            val systemTrash = trashManager.getSystemTrashItems()
            val quarantineItems = trashManager.getQuarantinedItems()
            _trashItems.value = systemTrash + quarantineItems
        }
    }

    fun toggleDuplicateSelection(fileKey: String) {
        _selectedDuplicates.value = _selectedDuplicates.value.let { selected ->
            if (fileKey in selected) selected - fileKey else selected + fileKey
        }
    }

    fun selectAllDuplicatesInGroup(group: DuplicateGroup) {
        val keys = group.files.drop(1).map { "${group.hash}_${it.uri}" }
        _selectedDuplicates.value = _selectedDuplicates.value + keys
    }

    fun clearDuplicateSelection() {
        _selectedDuplicates.value = emptySet()
    }

    fun toggleTrashSelection(itemKey: String) {
        _selectedTrash.value = _selectedTrash.value.let { selected ->
            if (itemKey in selected) selected - itemKey else selected + itemKey
        }
    }

    fun clearTrashSelection() {
        _selectedTrash.value = emptySet()
    }

    fun deleteSelectedDuplicates(groups: List<DuplicateGroup>) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            var deleted = 0
            var failed = 0
            val selected = _selectedDuplicates.value

            for (group in groups) {
                for (file in group.files.drop(1)) {
                    val key = "${group.hash}_${file.uri}"
                    if (key in selected) {
                        val success = trashManager.moveToTrash(file)
                        if (success) deleted++ else failed++
                    }
                }
            }

            _selectedDuplicates.value = emptySet()
            _deleteState.value = DeleteState.Complete(deleted, failed)
            scanner.scan()
        }
    }

    fun deleteSelectedTrashItems() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            var deleted = 0
            var failed = 0
            val selected = _selectedTrash.value

            for (item in _trashItems.value) {
                val key = item.file.uri.toString()
                if (key in selected) {
                    val success = trashManager.permanentlyDelete(item.file)
                    if (success) deleted++ else failed++
                }
            }

            _selectedTrash.value = emptySet()
            _deleteState.value = DeleteState.Complete(deleted, failed)
            loadTrashItems()
        }
    }

    fun restoreTrashItem(item: TrashItem) {
        viewModelScope.launch {
            trashManager.restoreFromTrash(item)
            loadTrashItems()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            var deleted = 0
            var failed = 0

            for (item in _trashItems.value) {
                val success = trashManager.permanentlyDelete(item.file)
                if (success) deleted++ else failed++
            }

            _deleteState.value = DeleteState.Complete(deleted, failed)
            loadTrashItems()
        }
    }

    fun clearDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    fun updatePermissionState(state: PermissionState) {
        _permissionState.value = state
    }
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data class Complete(val deleted: Int, val failed: Int) : DeleteState()
}

sealed class PermissionState {
    data object Unknown : PermissionState()
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object NeedsRationale : PermissionState()
    data object NeedsManageStorage : PermissionState()
}
