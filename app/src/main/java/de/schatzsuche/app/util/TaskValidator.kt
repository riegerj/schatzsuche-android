package de.schatzsuche.app.util

import de.schatzsuche.app.data.model.PostScanTask
import de.schatzsuche.app.data.model.PostScanTaskType
import de.schatzsuche.app.data.model.TaskResponse

object TaskValidator {
    fun isTaskComplete(task: PostScanTask, response: TaskResponse?): Boolean {
        if (!task.required) return true
        if (response == null) return false

        return when (task.type) {
            PostScanTaskType.TEXT_INPUT -> {
                val answer = response.textAnswer?.trim().orEmpty()
                if (task.correctAnswers.isEmpty()) answer.isNotBlank()
                else task.correctAnswers.any { it.equals(answer, ignoreCase = true) }
            }
            PostScanTaskType.SINGLE_CHOICE -> {
                val selected = response.selectedOptions.firstOrNull() ?: return false
                if (task.correctAnswers.isEmpty()) selected.isNotBlank()
                else task.correctAnswers.contains(selected)
            }
            PostScanTaskType.MULTIPLE_CHOICE -> {
                val selected = response.selectedOptions.toSet()
                if (task.correctAnswers.isEmpty()) selected.isNotEmpty()
                else selected == task.correctAnswers.toSet()
            }
            PostScanTaskType.PHOTO,
            PostScanTaskType.VIDEO,
            PostScanTaskType.AUDIO -> !response.mediaPath.isNullOrBlank()
        }
    }

    fun areAllTasksComplete(tasks: List<PostScanTask>, responses: Map<String, TaskResponse>): Boolean {
        return tasks.all { task -> isTaskComplete(task, responses[task.id]) }
    }
}
