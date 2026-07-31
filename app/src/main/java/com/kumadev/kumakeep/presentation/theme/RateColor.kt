package com.kumadev.kumakeep.presentation.theme

import androidx.compose.ui.graphics.Color
import com.kumadev.kumakeep.domain.model.UserRate

/**
 * Voti nell'ordine in cui vanno presentati all'utente: dal migliore al peggiore.
 * NOT_RATED è escluso di proposito — non è un voto, è l'assenza di voto.
 */
val RateFilterOrder = listOf(
    UserRate.WOW,
    UserRate.TOP,
    UserRate.YES,
    UserRate.MID,
    UserRate.BAH
)

/** Colore identificativo del voto (chip di filtro, badge). */
fun UserRate.toComposeColor(): Color = when (this) {
    UserRate.WOW -> RatingWow
    UserRate.TOP -> RatingTop
    UserRate.YES -> RatingYes
    UserRate.MID -> RatingMid
    UserRate.BAH -> RatingBah
    UserRate.NOT_RATED -> SurfaceDark
}

/** Colore del testo sopra [toComposeColor]: oro e grigio chiaro sono sfondi chiari. */
fun UserRate.toOnColor(): Color = when (this) {
    UserRate.WOW, UserRate.MID -> BackgroundDeep
    else -> TextPrimary
}

/** Etichetta breve mostrata all'utente, coerente con lo sheet di valutazione. */
fun UserRate.toShortLabel(): String = when (this) {
    UserRate.WOW -> "Wow"
    UserRate.TOP -> "Top"
    UserRate.YES -> "Sì!"
    UserRate.MID -> "Ok"
    UserRate.BAH -> "Meh"
    UserRate.NOT_RATED -> "Non valutato"
}
