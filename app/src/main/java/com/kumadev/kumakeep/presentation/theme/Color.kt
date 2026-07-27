package com.kumadev.kumakeep.presentation.theme

import androidx.compose.ui.graphics.Color

// Palette KumaKeep — estratta dal logo
val BackgroundDeep = Color(0xFF1C2D3A)
val SurfaceDark = Color(0xFF2A3F52)
val SurfaceVariant = Color(0xFF1F3040)

val AccentOrange = Color(0xFFC4511A)
val AccentGreen = Color(0xFF2A7A5A)

val TextPrimary = Color(0xFFF0EDE8)
val TextSecondary = Color(0xFFB0BEC5)
val TextDisabled = Color(0xFF607080)

val RatingBah = Color(0xFF607080)
val RatingMid = Color(0xFF90A4AE)
val RatingYes = Color(0xFF2A7A5A)
val RatingTop = Color(0xFFC4511A)
val RatingWow = Color(0xFFFFD700)

// Build "dev" — colore ben distinto dalla palette prod (usato per icona app + banner DEV MODE)
val DevBannerRed = Color(0xFFD32F2F)

// Palette selezionabile per i tag utente (swatch nel picker/creazione tag).
// Formato hex salvato in TagEntity.colorHex, es. "#C4511A".
val TagColorPalette = listOf(
    "#C4511A", // AccentOrange
    "#2A7A5A", // AccentGreen
    "#4A6FA5", // blu
    "#8E5CB0", // viola
    "#C4384A", // rosso
    "#B08D2A", // ocra
    "#2A9DA5", // teal
    "#B0567E"  // rosa
)
val TagColorDefault = TagColorPalette.first()