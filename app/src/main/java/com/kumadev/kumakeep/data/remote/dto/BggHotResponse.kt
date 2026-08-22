package com.kumadev.kumakeep.data.remote.dto

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

// Hot list ufficiale BGG (`hot?type=boardgame`): fino a 50 item ordinati per
// rank (1 = il più "caldo" del momento), aggiornata da BGG un paio di volte al
// giorno. A differenza di `thing`, porta solo i dati minimi per una card
// (nome, anno, thumbnail) — niente descrizione/stats/immagine grande.
@Root(name = "items", strict = false)
data class BggHotResponse(
    @field:ElementList(inline = true, required = false, entry = "item")
    var items: MutableList<BggHotItemDto> = mutableListOf()
)

@Root(name = "item", strict = false)
data class BggHotItemDto(
    @field:Attribute(name = "id", required = false)
    var id: Long = 0,

    // Posizione nella hot list ufficiale BGG: 1 = il più "caldo".
    @field:Attribute(name = "rank", required = false)
    var rank: Int = 0,

    @field:Element(name = "thumbnail", required = false)
    var thumbnail: BggValueString? = null,

    @field:Element(name = "name", required = false)
    var name: BggNameDto? = null,

    @field:Element(name = "yearpublished", required = false)
    var yearPublished: BggValueInt? = null
)

@Root(name = "value", strict = false)
data class BggValueString(
    @field:Attribute(name = "value", required = false)
    var value: String = ""
)
