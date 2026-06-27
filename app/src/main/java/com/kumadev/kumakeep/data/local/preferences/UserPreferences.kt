package com.kumadev.kumakeep.data.local.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "kumakeep_prefs"
private const val KEY_USERNAME = "username"
private const val KEY_RECENTLY_VIEWED = "recently_viewed"
private const val KEY_CAROUSEL_SIZE = "carousel_size"
private const val MAX_RECENTLY_VIEWED = 10
const val DEFAULT_CAROUSEL_SIZE = 10

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Username ──────────────────────────────────────────────────────────────

    private val _usernameFlow = MutableStateFlow(
        prefs.getString(KEY_USERNAME, "") ?: ""
    )
    val usernameFlow: StateFlow<String> = _usernameFlow.asStateFlow()

    val username: String get() = _usernameFlow.value

    fun setUsername(name: String) {
        prefs.edit().putString(KEY_USERNAME, name).apply()
        _usernameFlow.value = name
    }

    // ── Carousel size ─────────────────────────────────────────────────────────

    private val _carouselSizeFlow = MutableStateFlow(
        prefs.getInt(KEY_CAROUSEL_SIZE, DEFAULT_CAROUSEL_SIZE)
    )
    val carouselSizeFlow: StateFlow<Int> = _carouselSizeFlow.asStateFlow()

    val carouselSize: Int get() = _carouselSizeFlow.value

    fun setCarouselSize(size: Int) {
        prefs.edit().putInt(KEY_CAROUSEL_SIZE, size).apply()
        _carouselSizeFlow.value = size
    }

    // ── Recently Viewed ───────────────────────────────────────────────────────

    private val _recentlyViewedFlow = MutableStateFlow(loadRecentlyViewed())
    val recentlyViewedIdsFlow: StateFlow<List<Long>> = _recentlyViewedFlow.asStateFlow()

    fun addRecentlyViewed(bggId: Long) {
        val current = _recentlyViewedFlow.value.toMutableList()
        current.remove(bggId)
        current.add(0, bggId)
        if (current.size > MAX_RECENTLY_VIEWED) current.removeAt(current.size - 1)
        prefs.edit().putString(KEY_RECENTLY_VIEWED, current.joinToString(",")).apply()
        _recentlyViewedFlow.value = current
    }

    private fun loadRecentlyViewed(): List<Long> {
        val raw = prefs.getString(KEY_RECENTLY_VIEWED, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }
}
