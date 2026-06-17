package de.schatzsuche.app.data.repository

import android.content.Context
import de.schatzsuche.app.data.dao.HuntSessionDao
import de.schatzsuche.app.data.dao.HuntStepDao
import de.schatzsuche.app.data.dao.QrCodeDao
import de.schatzsuche.app.data.dao.StepCompletionDao
import de.schatzsuche.app.data.dao.TreasureHuntDao
import de.schatzsuche.app.data.database.AppDatabase
import de.schatzsuche.app.data.model.HuntSessionEntity
import de.schatzsuche.app.data.model.HuntSessionStatus
import de.schatzsuche.app.data.model.HuntStepEntity
import de.schatzsuche.app.data.model.QrCodeEntity
import de.schatzsuche.app.data.model.StepCompletionEntity
import de.schatzsuche.app.data.model.TaskResponse
import de.schatzsuche.app.data.model.TreasureHuntEntity
import de.schatzsuche.app.data.model.toResponsesJson
import de.schatzsuche.app.util.PdfGenerator
import de.schatzsuche.app.util.QrCodeUtil
import kotlinx.coroutines.flow.Flow
import java.io.File

class SchatzsucheRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val appContext = context.applicationContext

    val qrCodeDao: QrCodeDao = db.qrCodeDao()
    val treasureHuntDao: TreasureHuntDao = db.treasureHuntDao()
    val huntStepDao: HuntStepDao = db.huntStepDao()
    val huntSessionDao: HuntSessionDao = db.huntSessionDao()
    val stepCompletionDao: StepCompletionDao = db.stepCompletionDao()

    fun observeQrCodes(): Flow<List<QrCodeEntity>> = qrCodeDao.observeAll()
    fun observeHunts(): Flow<List<TreasureHuntEntity>> = treasureHuntDao.observeAll()
    fun observeHunt(id: String): Flow<TreasureHuntEntity?> = treasureHuntDao.observeById(id)
    fun observeSteps(huntId: String): Flow<List<HuntStepEntity>> = huntStepDao.observeByHunt(huntId)
    fun observeSessions(): Flow<List<HuntSessionEntity>> = huntSessionDao.observeAll()
    fun observeSession(id: String): Flow<HuntSessionEntity?> = huntSessionDao.observeById(id)
    fun observeCompletions(sessionId: String): Flow<List<StepCompletionEntity>> =
        stepCompletionDao.observeBySession(sessionId)

    suspend fun getQrCodeCount(): Int = qrCodeDao.count()

    suspend fun ensureQrCodes(count: Int = 12) {
        val existing = qrCodeDao.count()
        if (existing >= count) return
        val codes = QrCodeUtil.generateCodes(count)
        qrCodeDao.insertAll(codes)
    }

    suspend fun regenerateQrCodes(count: Int): File {
        qrCodeDao.deleteAll()
        val codes = QrCodeUtil.generateCodes(count)
        qrCodeDao.insertAll(codes)
        return PdfGenerator.generateQrCardsPdf(appContext, codes)
    }

    suspend fun importQrCodesFromScans(
        count: Int,
        scans: List<Pair<Int, String>> // number -> rawPayload
    ): File {
        require(scans.size == count) { "Es müssen genau $count QR-Karten gescannt werden." }

        val numbers = scans.map { it.first }
        require(numbers.toSet().size == numbers.size) { "Doppelte Kartennummer gescannt." }
        require(numbers.all { it in 1..count }) { "Ungültige Kartennummer gescannt." }

        val codes = scans
            .sortedBy { it.first }
            .map { (number, rawPayload) ->
                val codeId = QrCodeUtil.parsePayload(rawPayload)
                    ?: throw IllegalArgumentException("Falscher QR-Code für #${number}. Bitte die passende gedruckte Karte scannen.")
                QrCodeEntity(
                    codeId = codeId,
                    number = number,
                    payload = rawPayload
                )
            }

        val codeIds = codes.map { it.codeId }
        require(codeIds.toSet().size == codeIds.size) { "Eine QR-Karte wurde mehrfach gescannt." }

        qrCodeDao.deleteAll()
        qrCodeDao.insertAll(codes)
        return PdfGenerator.generateQrCardsPdf(appContext, codes)
    }

    suspend fun generatePdfFromExisting(): File {
        val codes = qrCodeDao.getAll()
        return PdfGenerator.generateQrCardsPdf(appContext, codes)
    }

    suspend fun saveHunt(hunt: TreasureHuntEntity) = treasureHuntDao.insert(hunt)
    suspend fun updateHunt(hunt: TreasureHuntEntity) = treasureHuntDao.update(hunt)
    suspend fun deleteHunt(id: String) {
        huntStepDao.deleteByHunt(id)
        treasureHuntDao.delete(id)
    }

    suspend fun saveStep(step: HuntStepEntity) = huntStepDao.insert(step)
    suspend fun updateStep(step: HuntStepEntity) = huntStepDao.update(step)
    suspend fun deleteStep(id: String) {
        val step = huntStepDao.getById(id)
        huntStepDao.delete(id)
        step?.let { normalizeStepOrder(it.huntId) }
    }
    suspend fun getSteps(huntId: String) = huntStepDao.getByHunt(huntId)
    suspend fun reorderSteps(huntId: String, fromIndex: Int, toIndex: Int) {
        val steps = huntStepDao.getByHunt(huntId).toMutableList()
        if (fromIndex !in steps.indices || toIndex !in steps.indices || fromIndex == toIndex) return
        val moved = steps.removeAt(fromIndex)
        steps.add(toIndex, moved)
        huntStepDao.insertAll(steps.mapIndexed { index, step -> step.copy(orderIndex = index) })
    }
    suspend fun getQrCodeByPayload(payload: String) = qrCodeDao.getByPayload(payload)
    suspend fun getQrCodeById(id: String) = qrCodeDao.getById(id)

    suspend fun startSession(huntId: String, participantName: String): HuntSessionEntity {
        val session = HuntSessionEntity(
            huntId = huntId,
            participantName = participantName,
            status = HuntSessionStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )
        huntSessionDao.insert(session)
        return session
    }

    suspend fun cancelSession(sessionId: String) {
        val session = huntSessionDao.getById(sessionId) ?: return
        huntSessionDao.update(
            session.copy(
                status = HuntSessionStatus.CANCELLED,
                finishedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeStep(
        session: HuntSessionEntity,
        step: HuntStepEntity,
        stepStartedAt: Long,
        responses: List<TaskResponse>
    ): HuntSessionEntity {
        val steps = huntStepDao.getByHunt(session.huntId)
        val currentIndex = steps.indexOfFirst { it.id == step.id }.coerceAtLeast(0)
        stepCompletionDao.insert(
            StepCompletionEntity(
                sessionId = session.id,
                stepId = step.id,
                stepIndex = currentIndex,
                startedAt = stepStartedAt,
                completedAt = System.currentTimeMillis(),
                taskResponsesJson = responses.toResponsesJson()
            )
        )
        val nextIndex = currentIndex + 1
        val isLast = nextIndex >= steps.size
        val updated = session.copy(
            currentStepIndex = nextIndex,
            status = if (isLast) HuntSessionStatus.COMPLETED else HuntSessionStatus.IN_PROGRESS,
            finishedAt = if (isLast) System.currentTimeMillis() else null
        )
        huntSessionDao.update(updated)
        return updated
    }

    suspend fun getSessionDetails(sessionId: String): SessionDetails? {
        val session = huntSessionDao.getById(sessionId) ?: return null
        val hunt = treasureHuntDao.getById(session.huntId) ?: return null
        val steps = huntStepDao.getByHunt(session.huntId)
        val completions = stepCompletionDao.getBySession(sessionId)
        return SessionDetails(session, hunt, steps, completions)
    }

    private suspend fun normalizeStepOrder(huntId: String) {
        val orderedSteps = huntStepDao.getByHunt(huntId)
        val normalized = orderedSteps.mapIndexed { index, s ->
            if (s.orderIndex == index) s else s.copy(orderIndex = index)
        }
        if (normalized.any { step -> step.orderIndex != orderedSteps.find { it.id == step.id }?.orderIndex }) {
            huntStepDao.insertAll(normalized)
        }
    }
}

data class SessionDetails(
    val session: HuntSessionEntity,
    val hunt: TreasureHuntEntity,
    val steps: List<HuntStepEntity>,
    val completions: List<StepCompletionEntity>
)
