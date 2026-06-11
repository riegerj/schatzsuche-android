package de.schatzsuche.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.schatzsuche.app.data.model.ContentBlockType
import de.schatzsuche.app.data.model.HuntSessionEntity
import de.schatzsuche.app.data.model.HuntSessionStatus
import de.schatzsuche.app.data.model.HuntStepEntity
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.data.model.PostScanTask
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.QrCodeEntity
import de.schatzsuche.app.data.model.RichContentBlock
import de.schatzsuche.app.data.model.TaskResponse
import de.schatzsuche.app.data.model.TreasureHuntEntity
import de.schatzsuche.app.data.model.toContentBlocks
import de.schatzsuche.app.data.model.toJson
import de.schatzsuche.app.data.model.toPostScanTasks
import de.schatzsuche.app.data.model.toResponsesJson
import de.schatzsuche.app.data.model.toTaskResponses
import de.schatzsuche.app.data.model.toTasksJson
import de.schatzsuche.app.data.repository.SessionDetails
import de.schatzsuche.app.data.repository.SchatzsucheRepository
import de.schatzsuche.app.util.MediaStorage
import de.schatzsuche.app.util.QrCodeUtil
import de.schatzsuche.app.util.TaskValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Setup ---

class SetupViewModel(private val repository: SchatzsucheRepository) : ViewModel() {
    private val _qrCount = MutableStateFlow(12)
    val qrCount: StateFlow<Int> = _qrCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pdfFile = MutableStateFlow<File?>(null)
    val pdfFile: StateFlow<File?> = _pdfFile.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val qrCodes = repository.observeQrCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQrCount(count: Int) {
        _qrCount.value = count.coerceIn(6, 60)
    }

    fun generateAndSave(regenerate: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = if (regenerate) {
                    repository.regenerateQrCodes(_qrCount.value)
                } else {
                    repository.ensureQrCodes(_qrCount.value)
                    repository.generatePdfFromExisting()
                }
                _pdfFile.value = file
                _message.value = "PDF erstellt: ${file.name}"
            } catch (e: Exception) {
                _message.value = "Fehler: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() { _message.value = null }
}

class SetupViewModelFactory(private val repository: SchatzsucheRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SetupViewModel(repository) as T
}

// --- Admin ---

class AdminViewModel(private val repository: SchatzsucheRepository) : ViewModel() {
    val hunts = repository.observeHunts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qrCodes = repository.observeQrCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeSteps(huntId: String) = repository.observeSteps(huntId)

    fun createHunt(title: String, theme: HuntTheme, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val hunt = TreasureHuntEntity(title = title, theme = theme)
            repository.saveHunt(hunt)
            onCreated(hunt.id)
        }
    }

    fun updateHunt(hunt: TreasureHuntEntity) {
        viewModelScope.launch {
            repository.updateHunt(hunt.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteHunt(id: String) {
        viewModelScope.launch { repository.deleteHunt(id) }
    }

    fun addStep(huntId: String, qrCodeId: String) {
        viewModelScope.launch {
            val steps = repository.getSteps(huntId)
            val step = HuntStepEntity(
                huntId = huntId,
                orderIndex = steps.size,
                title = "Schritt ${steps.size + 1}",
                instructionJson = listOf(
                    RichContentBlock(type = ContentBlockType.TEXT, text = "Beschreibe hier das Rätsel…")
                ).toJson(),
                qrCodeId = qrCodeId,
                isFinalStep = false
            )
            repository.saveStep(step)
        }
    }

    fun deleteStep(stepId: String) {
        viewModelScope.launch { repository.deleteStep(stepId) }
    }

    fun saveStep(step: HuntStepEntity) {
        viewModelScope.launch { repository.saveStep(step) }
    }

    suspend fun getStep(stepId: String) = repository.huntStepDao.getById(stepId)
    suspend fun getHunt(huntId: String) = repository.treasureHuntDao.getById(huntId)
    suspend fun getQrCode(codeId: String) = repository.getQrCodeById(codeId)
}

class AdminViewModelFactory(private val repository: SchatzsucheRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(repository) as T
}

// --- Participant ---

class ParticipantViewModel(private val repository: SchatzsucheRepository) : ViewModel() {
    val hunts = repository.observeHunts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSession(huntId: String, name: String, onStarted: (String) -> Unit) {
        viewModelScope.launch {
            val session = repository.startSession(huntId, name.trim())
            onStarted(session.id)
        }
    }
}

class ParticipantViewModelFactory(private val repository: SchatzsucheRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ParticipantViewModel(repository) as T
}

// --- Play ---

enum class PlayPhase {
    INSTRUCTION,
    SCAN,
    POST_TASKS,
    TREASURE,
    COMPLETED
}

data class PlayUiState(
    val phase: PlayPhase = PlayPhase.INSTRUCTION,
    val currentStep: HuntStepEntity? = null,
    val qrCode: QrCodeEntity? = null,
    val taskResponses: Map<String, TaskResponse> = emptyMap(),
    val scanError: String? = null,
    val taskError: String? = null,
    val stepStartedAt: Long = System.currentTimeMillis(),
    val completedCount: Int = 0,
    val totalSteps: Int = 0,
    val huntTheme: HuntTheme = HuntTheme.CLASSIC
)

class PlayViewModel(
    private val repository: SchatzsucheRepository,
    private val sessionId: String
) : ViewModel() {
    val session = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completions = repository.observeCompletions(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    private var steps: List<HuntStepEntity> = emptyList()
    private var huntTheme: HuntTheme = HuntTheme.CLASSIC

    init {
        viewModelScope.launch { loadGame() }
    }

    private suspend fun loadGame() {
        val sess = repository.huntSessionDao.getById(sessionId) ?: return
        val hunt = repository.treasureHuntDao.getById(sess.huntId) ?: return
        steps = repository.getSteps(sess.huntId)
        huntTheme = hunt.theme
        val step = steps.getOrNull(sess.currentStepIndex)
        val qr = step?.let { repository.getQrCodeById(it.qrCodeId) }
        _uiState.value = PlayUiState(
            phase = if (sess.status == HuntSessionStatus.COMPLETED) PlayPhase.COMPLETED
            else PlayPhase.INSTRUCTION,
            currentStep = step,
            qrCode = qr,
            completedCount = sess.currentStepIndex,
            totalSteps = steps.size,
            huntTheme = hunt.theme,
            stepStartedAt = System.currentTimeMillis()
        )
    }

    fun showScanner() {
        _uiState.value = _uiState.value.copy(phase = PlayPhase.SCAN, scanError = null)
    }

    fun showInstruction() {
        _uiState.value = _uiState.value.copy(phase = PlayPhase.INSTRUCTION, scanError = null)
    }

    fun onQrScanned(rawPayload: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val step = state.currentStep ?: return@launch
            val codeId = QrCodeUtil.parsePayload(rawPayload)
            if (codeId == null || codeId != step.qrCodeId) {
                _uiState.value = state.copy(scanError = "Falscher QR-Code! Versuche es nochmal.")
                return@launch
            }
            val tasks = step.postScanTasksJson.toPostScanTasks()
            if (tasks.isNotEmpty()) {
                _uiState.value = state.copy(
                    phase = PlayPhase.POST_TASKS,
                    scanError = null,
                    taskResponses = emptyMap()
                )
            } else if (step.isFinalStep) {
                _uiState.value = state.copy(phase = PlayPhase.TREASURE, scanError = null)
            } else {
                advanceOrFinish()
            }
        }
    }

    fun updateTaskResponse(task: PostScanTask, response: TaskResponse) {
        val updated = _uiState.value.taskResponses.toMutableMap()
        updated[task.id] = response
        _uiState.value = _uiState.value.copy(taskResponses = updated, taskError = null)
    }

    fun submitPostTasks() {
        val state = _uiState.value
        val step = state.currentStep ?: return
        val tasks = step.postScanTasksJson.toPostScanTasks()
        if (!TaskValidator.areAllTasksComplete(tasks, state.taskResponses)) {
            _uiState.value = state.copy(taskError = "Bitte alle Aufgaben erfüllen.")
            return
        }
        if (step.isFinalStep) {
            _uiState.value = state.copy(phase = PlayPhase.TREASURE)
        } else {
            viewModelScope.launch {
                advanceOrFinish(state.taskResponses.values.toList())
            }
        }
    }

    private suspend fun advanceOrFinish(responses: List<TaskResponse> = emptyList()) {
        val sess = repository.huntSessionDao.getById(sessionId) ?: return
        val step = _uiState.value.currentStep ?: return
        if (step.isFinalStep && _uiState.value.phase != PlayPhase.TREASURE && responses.isEmpty()) {
            _uiState.value = _uiState.value.copy(phase = PlayPhase.TREASURE)
            return
        }
        val updated = repository.completeStep(
            session = sess,
            step = step,
            stepStartedAt = _uiState.value.stepStartedAt,
            responses = responses
        )
        if (updated.status == HuntSessionStatus.COMPLETED) {
            _uiState.value = _uiState.value.copy(phase = PlayPhase.COMPLETED, completedCount = steps.size)
        } else {
            val nextStep = steps.getOrNull(updated.currentStepIndex)
            val qr = nextStep?.let { repository.getQrCodeById(it.qrCodeId) }
            _uiState.value = PlayUiState(
                phase = PlayPhase.INSTRUCTION,
                currentStep = nextStep,
                qrCode = qr,
                completedCount = updated.currentStepIndex,
                totalSteps = steps.size,
                huntTheme = huntTheme,
                stepStartedAt = System.currentTimeMillis()
            )
        }
    }

    fun finishTreasure() {
        viewModelScope.launch {
            advanceOrFinish(_uiState.value.taskResponses.values.toList())
        }
    }

    fun cancelSession(onCancelled: () -> Unit) {
        viewModelScope.launch {
            repository.cancelSession(sessionId)
            onCancelled()
        }
    }
}

class PlayViewModelFactory(
    private val repository: SchatzsucheRepository,
    private val sessionId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlayViewModel(repository, sessionId) as T
}

// --- Summary ---

class SummaryViewModel(
    private val repository: SchatzsucheRepository,
    private val sessionId: String
) : ViewModel() {
    private val _details = MutableStateFlow<SessionDetails?>(null)
    val details: StateFlow<SessionDetails?> = _details.asStateFlow()

    private val qrCodeMap = MutableStateFlow<Map<String, QrCodeEntity>>(emptyMap())

    init {
        viewModelScope.launch {
            _details.value = repository.getSessionDetails(sessionId)
            val codes = repository.qrCodeDao.getAll().associateBy { it.codeId }
            qrCodeMap.value = codes
        }
    }

    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val min = seconds / 60
        val sec = seconds % 60
        return if (min > 0) "${min} Min ${sec} Sek" else "${sec} Sek"
    }

    fun formatDateTime(ts: Long): String {
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        return fmt.format(Date(ts))
    }
}

class SummaryViewModelFactory(
    private val repository: SchatzsucheRepository,
    private val sessionId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SummaryViewModel(repository, sessionId) as T
}

// --- Step Edit State ---

data class StepEditState(
    val step: HuntStepEntity? = null,
    val contentBlocks: List<RichContentBlock> = emptyList(),
    val postScanTasks: List<PostScanTask> = emptyList(),
    val availableQrCodes: List<QrCodeEntity> = emptyList(),
    val selectedQrCode: QrCodeEntity? = null
)

class StepEditViewModel(
    private val repository: SchatzsucheRepository,
    private val huntId: String,
    private val stepId: String
) : ViewModel() {
    private val _state = MutableStateFlow(StepEditState())
    val state: StateFlow<StepEditState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val step = repository.huntStepDao.getById(stepId)
            val codes = repository.qrCodeDao.getAll()
            val selected = step?.let { repository.getQrCodeById(it.qrCodeId) }
            _state.value = StepEditState(
                step = step,
                contentBlocks = step?.instructionJson?.toContentBlocks() ?: emptyList(),
                postScanTasks = step?.postScanTasksJson?.toPostScanTasks() ?: emptyList(),
                availableQrCodes = codes,
                selectedQrCode = selected
            )
        }
    }

    fun updateTitle(title: String) {
        val step = _state.value.step ?: return
        _state.value = _state.value.copy(step = step.copy(title = title))
    }

    fun updateTreasureHint(hint: String) {
        val step = _state.value.step ?: return
        _state.value = _state.value.copy(step = step.copy(treasureHint = hint, isFinalStep = true))
    }

    fun setFinalStep(isFinal: Boolean) {
        val step = _state.value.step ?: return
        _state.value = _state.value.copy(step = step.copy(isFinalStep = isFinal))
    }

    fun selectQrCode(code: QrCodeEntity) {
        val step = _state.value.step ?: return
        _state.value = _state.value.copy(step = step.copy(qrCodeId = code.codeId), selectedQrCode = code)
    }

    fun addTextBlock() {
        val blocks = _state.value.contentBlocks + RichContentBlock(type = ContentBlockType.TEXT, text = "")
        _state.value = _state.value.copy(contentBlocks = blocks)
    }

    fun updateTextBlock(id: String, text: String) {
        val blocks = _state.value.contentBlocks.map {
            if (it.id == id) it.copy(text = text) else it
        }
        _state.value = _state.value.copy(contentBlocks = blocks)
    }

    fun addMediaBlock(context: Context, uri: Uri, type: ContentBlockType) {
        val ext = when (type) {
            ContentBlockType.IMAGE -> "jpg"
            ContentBlockType.AUDIO -> "m4a"
            ContentBlockType.VIDEO -> "mp4"
            else -> "dat"
        }
        val path = MediaStorage.copyToAppStorage(context, uri, "instructions", ext)
        val blocks = _state.value.contentBlocks + RichContentBlock(type = type, mediaPath = path)
        _state.value = _state.value.copy(contentBlocks = blocks)
    }

    fun removeBlock(id: String) {
        val block = _state.value.contentBlocks.find { it.id == id }
        MediaStorage.deleteIfExists(block?.mediaPath)
        _state.value = _state.value.copy(contentBlocks = _state.value.contentBlocks.filter { it.id != id })
    }

    fun addPostScanTask(type: PostScanTaskType) {
        val task = PostScanTask(
            type = type,
            question = when (type) {
                PostScanTaskType.TEXT_INPUT -> "Beantworte die Frage:"
                PostScanTaskType.SINGLE_CHOICE -> "Wähle die richtige Antwort:"
                PostScanTaskType.MULTIPLE_CHOICE -> "Wähle alle richtigen Antworten:"
                PostScanTaskType.PHOTO -> "Mache ein Foto:"
                PostScanTaskType.VIDEO -> "Nimm ein Video auf:"
                PostScanTaskType.AUDIO -> "Nimm eine Sprachnachricht auf:"
            }
        )
        _state.value = _state.value.copy(postScanTasks = _state.value.postScanTasks + task)
    }

    fun updatePostScanTask(task: PostScanTask) {
        val tasks = _state.value.postScanTasks.map { if (it.id == task.id) task else it }
        _state.value = _state.value.copy(postScanTasks = tasks)
    }

    fun removePostScanTask(id: String) {
        _state.value = _state.value.copy(postScanTasks = _state.value.postScanTasks.filter { it.id != id })
    }

    fun save() {
        viewModelScope.launch {
            val current = _state.value
            val step = current.step ?: return@launch
            repository.saveStep(
                step.copy(
                    instructionJson = current.contentBlocks.toJson(),
                    postScanTasksJson = current.postScanTasks.toTasksJson()
                )
            )
        }
    }
}

class StepEditViewModelFactory(
    private val repository: SchatzsucheRepository,
    private val huntId: String,
    private val stepId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        StepEditViewModel(repository, huntId, stepId) as T
}
