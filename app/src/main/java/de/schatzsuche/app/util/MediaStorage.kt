package de.schatzsuche.app.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import de.schatzsuche.app.data.model.ContentBlockType
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaStorage {
    fun fileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyToAppStorage(
        context: Context,
        sourceUri: Uri,
        subfolder: String,
        type: ContentBlockType
    ): String? {
        val extension = extensionForType(context, sourceUri, type)
        val dir = File(context.filesDir, subfolder).apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.$extension")
        val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
            dest.length() > 0L
        } ?: false
        return if (copied) dest.absolutePath else null.also { dest.delete() }
    }

    private fun extensionForType(context: Context, sourceUri: Uri, type: ContentBlockType): String {
        val mimeType = context.contentResolver.getType(sourceUri)
        val fromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return when (type) {
            ContentBlockType.IMAGE -> fromMime?.takeIf { it.isNotBlank() } ?: "jpg"
            ContentBlockType.AUDIO -> fromMime?.takeIf { it.isNotBlank() } ?: "m4a"
            ContentBlockType.VIDEO -> fromMime?.takeIf { it.isNotBlank() } ?: "mp4"
            else -> fromMime?.takeIf { it.isNotBlank() } ?: "dat"
        }
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
