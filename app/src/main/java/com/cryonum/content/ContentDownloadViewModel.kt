package com.cryonum.content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContentDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val dependencies = ContentDependencies.get(application)
    private val repository = dependencies.repository
    private val coordinator = dependencies.coordinator
    private val mutableState = MutableStateFlow(ContentDownloadUiState())
    val uiState: StateFlow<ContentDownloadUiState> = mutableState.asStateFlow()

    private var manifest: ContentManifest? = null
    private var referenceScreenEntered = false
    private var lecturePromptDismissed = false
    private var openPolicyAfterDownload = false
    private val observations = mutableListOf<Pair<LiveData<List<WorkInfo>>, Observer<List<WorkInfo>>>>()
    private val observedBundles = mutableSetOf<ContentBundle>()

    fun enterReferenceScreen() {
        ensureObserving(ContentBundle.LECTURES)
        referenceScreenEntered = true
        refreshManifestAndFiles()
    }

    fun refreshManifestAndFiles() {
        viewModelScope.launch {
            mutableState.update { it.copy(checkingManifest = true, errorCategory = null) }
            try {
                val verified = repository.manifest()
                manifest = verified
                val lectures = repository.localSummary(verified, ContentBundle.LECTURES)
                val policy = repository.localSummary(verified, ContentBundle.PRIVACY_POLICY)
                mutableState.update {
                    it.copy(
                        checkingManifest = false,
                        manifestReady = true,
                        manifestRevision = verified.revision,
                        lectureCount = lectures.totalCount,
                        lecturesDownloaded = lectures.verifiedCount,
                        lecturesTotalBytes = lectures.totalBytes,
                        policySizeBytes = verified.privacyPolicy.sizeBytes,
                        showLecturePrompt = referenceScreenEntered && !lectures.current && !lecturePromptDismissed && !it.workActive,
                        errorCategory = null
                    )
                }
                if (openPolicyAfterDownload && policy.current) {
                    repository.verifiedLocalFile(verified, verified.privacyPolicy)?.let { file ->
                        openPolicyAfterDownload = false
                        mutableState.update {
                            it.copy(
                                openPdfPath = file.absolutePath,
                                openPdfContentVersion = verified.privacyPolicy.contentVersion.toIntOrNull(),
                                progressVisible = false
                            )
                        }
                    }
                }
            } catch (e: ContentException) {
                mutableState.update { it.copy(checkingManifest = false, manifestReady = false, errorCategory = e.category) }
            } catch (_: Exception) {
                mutableState.update { it.copy(checkingManifest = false, manifestReady = false, errorCategory = ContentErrorCategory.UNKNOWN) }
            }
        }
    }

    fun dismissLecturePrompt() {
        lecturePromptDismissed = true
        mutableState.update { it.copy(showLecturePrompt = false) }
    }

    fun startLectures() {
        lecturePromptDismissed = true
        mutableState.update { it.copy(showLecturePrompt = false, progressVisible = true, errorCategory = null, activeBundle = ContentBundle.LECTURES, phase = "ENQUEUED") }
        coordinator.enqueue(ContentBundle.LECTURES)
    }

    fun requestPolicy() {
        ensureObserving(ContentBundle.PRIVACY_POLICY)
        viewModelScope.launch {
            mutableState.update { it.copy(checkingManifest = true, errorCategory = null) }
            try {
                val verified = repository.manifest()
                manifest = verified
                val file = verified.privacyPolicy
                val local = repository.verifiedLocalFile(verified, file)
                if (local != null) {
                    mutableState.update {
                        it.copy(
                            checkingManifest = false,
                            manifestReady = true,
                            openPdfPath = local.absolutePath,
                            openPdfContentVersion = file.contentVersion.toIntOrNull(),
                            policySizeBytes = file.sizeBytes,
                            activeBundle = null,
                            progressVisible = false,
                            phase = "COMPLETED"
                        )
                    }
                } else {
                    mutableState.update {
                        it.copy(
                            checkingManifest = false,
                            manifestReady = true,
                            manifestRevision = verified.revision,
                            policySizeBytes = file.sizeBytes,
                            showPolicyPrompt = true,
                            policyIsUpdate = repository.hasStoredFile(file)
                        )
                    }
                }
            } catch (e: ContentException) {
                mutableState.update {
                    it.copy(
                        checkingManifest = false,
                        errorCategory = e.category,
                        activeBundle = ContentBundle.PRIVACY_POLICY,
                        phase = "FAILED",
                        progressVisible = true
                    )
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        checkingManifest = false,
                        errorCategory = ContentErrorCategory.UNKNOWN,
                        activeBundle = ContentBundle.PRIVACY_POLICY,
                        phase = "FAILED",
                        progressVisible = true
                    )
                }
            }
        }
    }

    fun dismissPolicyPrompt() = mutableState.update { it.copy(showPolicyPrompt = false) }

    fun startPolicy() {
        openPolicyAfterDownload = true
        mutableState.update { it.copy(showPolicyPrompt = false, progressVisible = true, errorCategory = null, activeBundle = ContentBundle.PRIVACY_POLICY, phase = "ENQUEUED") }
        coordinator.enqueue(ContentBundle.PRIVACY_POLICY)
    }

    fun openLecture(order: Int) {
        viewModelScope.launch {
            val verified = manifest
            if (verified == null) {
                refreshManifestAndFiles()
                return@launch
            }
            val entry = verified.lectures.firstOrNull { it.order == order } ?: return@launch
            val local = repository.verifiedLocalFile(verified, entry)
            if (local != null) mutableState.update { it.copy(openPdfPath = local.absolutePath) }
            else mutableState.update { it.copy(showLecturePrompt = true) }
        }
    }

    fun consumeOpenPdf() = mutableState.update { it.copy(openPdfPath = null, openPdfContentVersion = null) }
    fun hideProgress() = mutableState.update { it.copy(progressVisible = false) }
    fun showProgress() = mutableState.update {
        if (it.activeBundle != null) it.copy(progressVisible = true)
        else if (it.errorCategory != null) it.copy(
            activeBundle = if (referenceScreenEntered) ContentBundle.LECTURES else ContentBundle.PRIVACY_POLICY,
            phase = "FAILED",
            progressVisible = true
        ) else it
    }

    fun cancel() {
        mutableState.value.activeBundle?.let(coordinator::cancel)
        mutableState.update { it.copy(progressVisible = false) }
    }

    fun retry() {
        val bundle = mutableState.value.activeBundle ?: if (referenceScreenEntered) ContentBundle.LECTURES else ContentBundle.PRIVACY_POLICY
        if (bundle == ContentBundle.PRIVACY_POLICY) openPolicyAfterDownload = true
        mutableState.update { it.copy(progressVisible = true, errorCategory = null, activeBundle = bundle, phase = "ENQUEUED") }
        coordinator.enqueue(bundle, replace = true)
    }

    fun restart() {
        val bundle = mutableState.value.activeBundle ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(progressVisible = true, errorCategory = null, phase = "PREPARING") }
            coordinator.restart(bundle)
        }
    }

    private fun observeWork(bundle: ContentBundle) {
        val liveData = coordinator.observe(bundle)
        val observer = Observer<List<WorkInfo>> { infos ->
            val info = infos.lastOrNull() ?: return@Observer
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> mutableState.update {
                    if (it.activeBundle != null && it.activeBundle != bundle) it else it.copy(activeBundle = bundle, phase = "ENQUEUED")
                }
                WorkInfo.State.RUNNING -> {
                    val data = info.progress
                    mutableState.update {
                        it.copy(
                            activeBundle = bundle,
                            phase = data.getString(ContentDownloadWorker.KEY_PHASE) ?: "PREPARING",
                            currentFileId = data.getString(ContentDownloadWorker.KEY_FILE_ID),
                            currentFileIndex = data.getInt(ContentDownloadWorker.KEY_FILE_INDEX, 0),
                            currentFileCount = data.getInt(ContentDownloadWorker.KEY_FILE_COUNT, 0),
                            currentFileBytes = data.getLong(ContentDownloadWorker.KEY_FILE_BYTES, 0),
                            currentFileTotalBytes = data.getLong(ContentDownloadWorker.KEY_FILE_TOTAL_BYTES, 0),
                            overallBytes = data.getLong(ContentDownloadWorker.KEY_OVERALL_BYTES, 0),
                            overallTotalBytes = data.getLong(ContentDownloadWorker.KEY_OVERALL_TOTAL_BYTES, 0),
                            completedFiles = data.getInt(ContentDownloadWorker.KEY_COMPLETED_FILES, 0)
                        )
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    mutableState.update { it.copy(activeBundle = null, phase = "COMPLETED", errorCategory = null) }
                    refreshManifestAndFiles()
                }
                WorkInfo.State.FAILED -> {
                    val category = info.outputData.getString(ContentDownloadWorker.KEY_ERROR_CATEGORY)
                        ?.let { runCatching { ContentErrorCategory.valueOf(it) }.getOrNull() }
                        ?: ContentErrorCategory.UNKNOWN
                    mutableState.update { it.copy(activeBundle = bundle, phase = "FAILED", errorCategory = category, progressVisible = true) }
                }
                WorkInfo.State.CANCELLED -> mutableState.update {
                    it.copy(activeBundle = bundle, phase = "CANCELLED", errorCategory = ContentErrorCategory.CANCELLED)
                }
            }
        }
        liveData.observeForever(observer)
        observations += liveData to observer
    }

    private fun ensureObserving(bundle: ContentBundle) {
        if (observedBundles.add(bundle)) observeWork(bundle)
    }

    override fun onCleared() {
        observations.forEach { (liveData, observer) -> liveData.removeObserver(observer) }
        super.onCleared()
    }
}
