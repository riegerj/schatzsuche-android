package de.schatzsuche.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaStorage {
    fun copyToAppStorage(context: Context, sourceUri: Uri, subfolder: String, extension: String): String? {
        val dir = File(context.filesDir, subfolder).apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.$extension")
        val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
            dest.length() > 0L
        } ?: false
        return if (copied) dest.absolutePath else null.also { dest.delete() }
    }

    fun createMediaFile(context: Context, subfolder: String, extension: String): File {
        val dir = File(context.filesDir, subfolder).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.$extension")
    }

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).delete()
    }
}
