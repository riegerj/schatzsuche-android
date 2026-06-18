package de.schatzsuche.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class HuntTheme(val displayName: String, val emoji: String) {
    PIRATES("Piraten", "🏴‍☠️"),
    SPACE("Weltraum", "🚀"),
    KNIGHTS("Ritter & Verlies", "⚔️"),
    EGYPT("Ägypten", "🏺"),
    CLASSIC("Klassisch", "🗺️"),
    JUNGLE("Dschungel", "🌴")
}

enum class PostScanTaskType {
    TEXT_INPUT,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    PHOTO,
    VIDEO,
    AUDIO
}

data class PostScanTask(
    val id: String = UUID.randomUUID().toString(),
    val type: PostScanTaskType,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val required: Boolean = true
)

data class RichContentBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: ContentBlockType,
    val text: String? = null,
    val mediaPath: String? = null
)

enum class ContentBlockType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO
}

@Entity(tableName = "qr_codes")
data class QrCodeEntity(
    @PrimaryKey val codeId: String,
    val number: Int,
    val payload: String
)

@Entity(tableName = "treasure_hunts")
data class TreasureHuntEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val theme: HuntTheme,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hunt_steps")
data class HuntStepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val huntId: String,
    val orderIndex: Int,
    val title: String,
    val instructionJson: String,
    val qrCodeId: String,
    val isFinalStep: Boolean = false,
    val treasureHint: String? = null,
    val postScanTasksJson: String = "[]"
)

enum class HuntSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "hunt_sessions")
data class HuntSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val huntId: String,
    val participantName: String,
    val status: HuntSessionStatus,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val currentStepIndex: Int = 0
)

@Entity(tableName = "step_completions")
data class StepCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val stepId: String,
    val stepIndex: Int,
    val startedAt: Long,
    val completedAt: Long,
    val taskResponsesJson: String = "[]"
)

data class TaskResponse(
    val taskId: String,
    val type: PostScanTaskType,
    val textAnswer: String? = null,
    val selectedOptions: List<String> = emptyList(),
    val mediaPath: String? = null
)
