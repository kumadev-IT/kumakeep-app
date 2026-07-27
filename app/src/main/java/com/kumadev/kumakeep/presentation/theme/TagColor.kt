package com.kumadev.kumakeep.presentation.theme

import androidx.compose.ui.graphics.Color
import com.kumadev.kumakeep.domain.model.Tag

/** Converte il colorHex salvato (es. "#C4511A") in un Color Compose, con fallback sicuro. */
fun Tag.toComposeColor(): Color = try {
    Color(android.graphics.Color.parseColor(colorHex))
} catch (e: IllegalArgumentException) {
    AccentOrange
}
