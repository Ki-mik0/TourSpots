package ua.chnu.tourspots.locations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Local replacement for the MinIO object storage from the technical task:
 * picked photos are copied into the app's private storage and the DB keeps the file path.
 */
class PhotoStorage(context: Context) {

    private val app = context.applicationContext
    private val pendingDir = File(app.cacheDir, "pending_photos").apply { mkdirs() }
    private val photosDir = File(app.filesDir, "photos").apply { mkdirs() }

    /** Copies a picked image into a temporary file (the form is not saved yet). */
    fun copyToPending(uri: Uri): String? = runCatching {
        val target = File(pendingDir, "${UUID.randomUUID()}.jpg")
        app.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        target.absolutePath
    }.getOrNull()

    /** Moves the pending files into permanent storage ("uploadToMinIO()") and returns new paths. */
    fun commit(pendingPaths: List<String>): List<String> = pendingPaths.mapNotNull { path ->
        val source = File(path)
        if (source.parentFile != pendingDir) return@mapNotNull path   // already permanent
        val target = File(photosDir, source.name)
        if (source.renameTo(target) || runCatching { source.copyTo(target, overwrite = true); source.delete() }.isSuccess) {
            target.absolutePath
        } else {
            null
        }
    }

    /** "deleteFromMinIO()": removes stored files. */
    fun delete(paths: List<String>) = paths.forEach { File(it).delete() }

    companion object {
        /** Decodes a downscaled bitmap so large camera photos do not exhaust memory. */
        fun loadThumbnail(path: String, targetPx: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
