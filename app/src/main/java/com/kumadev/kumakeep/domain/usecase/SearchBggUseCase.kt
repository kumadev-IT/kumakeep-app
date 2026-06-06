package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

class SearchBggUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(query: String): Result<List<SearchResult>> {
        if (query.isBlank()) return Result.success(emptyList())
        return repository.searchBgg(query)
    }
}