package com.kumadev.kumakeep.util

import android.net.Uri

/**
 * Singleton leggero per trasportare un PDF ricevuto via ACTION_SEND
 * dalla MainActivity al GameDetailViewModel del gioco scelto dall'utente.
 *
 * Il VM legge e azzera il holder nel proprio init, così ogni import
 * viene consumato una volta sola.
 */
object PendingPdfHolder {
    @Volatile var uri: Uri? = null
    @Volatile var fileName: String? = null

    fun consume(): Pair<Uri, String>? {
        val u = uri ?: return null
        val n = fileName ?: "regolamento.pdf"
        uri = null
        fileName = null
        return u to n
    }
}
