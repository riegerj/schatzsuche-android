package de.schatzsuche.app.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun List<RichContentBlock>.toJson(): String = gson.toJson(this)

fun String.toContentBlocks(): List<RichContentBlock> {
    if (isBlank() || this == "[]") return emptyList()
    val type = object : TypeToken<List<RichContentBlock>>() {}.type
    return gson.fromJson(this, type)
}

fun List<PostScanTask>.toTasksJson(): String = gson.toJson(this)

fun String.toPostScanTasks(): List<PostScanTask> {
    if (isBlank() || this == "[]") return emptyList()
    val type = object : TypeToken<List<PostScanTask>>() {}.type
    return gson.fromJson(this, type)
}

fun List<TaskResponse>.toResponsesJson(): String = gson.toJson(this)

fun String.toTaskResponses(): List<TaskResponse> {
    if (isBlank() || this == "[]") return emptyList()
    val type = object : TypeToken<List<TaskResponse>>() {}.type
    return gson.fromJson(this, type)
}
