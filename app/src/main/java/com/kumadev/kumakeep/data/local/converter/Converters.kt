package com.kumadev.kumakeep.data.local.converter

import androidx.room.TypeConverter
import com.kumadev.kumakeep.data.local.entity.NumPlays
import com.kumadev.kumakeep.data.local.entity.UserRate

class Converters {

    // --- UserRate ---
    @TypeConverter
    fun fromUserRate(value: UserRate): String = value.name

    @TypeConverter
    fun toUserRate(value: String): UserRate =
        UserRate.entries.firstOrNull { it.name == value } ?: UserRate.NOT_RATED

    // --- NumPlays ---
    @TypeConverter
    fun fromNumPlays(value: NumPlays): String = value.name

    @TypeConverter
    fun toNumPlays(value: String): NumPlays =
        NumPlays.entries.firstOrNull { it.name == value } ?: NumPlays.NOT_CLASSIFIED
}