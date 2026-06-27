package com.kumadev.kumakeep.data.rulebook

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulebookFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun rulebookDir(): File {
        val dir = File(context.filesDir, "rulebooks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun fileForGame(gameId: Long): File = File(rulebookDir(), "$gameId.pdf")

    suspend fun copyFromUri(uri: Uri, gameId: Long): File = withContext(Dispatchers.IO) {
        val dest = fileForGame(gameId)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Impossibile aprire il file selezionato")
        dest
    }

    fun delete(gameId: Long): Boolean = fileForGame(gameId).takeIf { it.exists() }?.delete() ?: false

    fun getPageCount(file: File): Int {
        if (!file.exists()) return 0
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            0
        }
    }
}
