package com.kumadev.kumakeep.network.ludomancer

import com.kumadev.rulesreader.ludomancer.IngestAck
import com.kumadev.rulesreader.ludomancer.LudomancerAnswer
import com.kumadev.rulesreader.ludomancer.LudomancerClient
import com.kumadev.rulesreader.ludomancer.LudomancerState
import com.kumadev.rulesreader.ludomancer.LudomancerStatus
import com.kumadev.rulesreader.ludomancer.LudomancerTurn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock temporaneo di [LudomancerClient]: NON fa alcuna chiamata di rete.
 *
 * Serve solo a fissare il contratto e la DI mentre il backend Ludomancer
 * viene sviluppato nel progetto separato. Ogni interrogazione risponde con
 * un placeholder "coming soon".
 *
 * Quando il backend sarà pronto: creare `LudomancerRestClient` (Retrofit/OkHttp,
 * pattern GeminiFlashClient) e cambiare il binding in [com.kumadev.kumakeep.di.LudomancerModule].
 */
@Singleton
class MockLudomancerClient @Inject constructor() : LudomancerClient {

    override suspend fun status(bggId: Long): LudomancerStatus =
        LudomancerStatus(state = LudomancerState.NOT_INGESTED, detail = WIP_MESSAGE)

    override suspend fun ingest(bggId: Long, rulebookPdf: File): IngestAck =
        IngestAck(jobId = "mock-$bggId", state = LudomancerState.NOT_INGESTED)

    override suspend fun ask(
        bggId: Long,
        question: String,
        history: List<LudomancerTurn>
    ): LudomancerAnswer =
        LudomancerAnswer(answer = WIP_MESSAGE, placeholder = true)

    override suspend fun feedback(bggId: Long, questionId: String, correction: String) {
        // no-op: nessun backend a cui inviare il feedback.
    }

    companion object {
        const val WIP_MESSAGE = "🔮 Work in Progress — Ludomancer coming soon."
    }
}
