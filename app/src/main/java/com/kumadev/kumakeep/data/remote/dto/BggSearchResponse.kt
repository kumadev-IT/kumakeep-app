package com.kumadev.kumakeep.data.remote.dto

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "items", strict = false)
data class BggSearchResponse(
    @field:ElementList(inline = true, required = false, entry = "item")
    var items: MutableList<BggSearchItemDto> = mutableListOf()
)

@Root(name = "item", strict = false)
data class BggSearchItemDto(
    @field:Attribute(name = "id", required = false)
    var id: Long = 0,

    // "boardgame" | "boardgameexpansion" | "boardgameaccessory".
    // Nel search NON è affidabile per riga (stesso id compare in più bucket):
    // si usa solo per l'appartenenza al bucket in fase di dedup.
    @field:Attribute(name = "type", required = false)
    var type: String = "",

    @field:Element(name = "name", required = false)
    var name: BggNameDto? = null,

    @field:Element(name = "yearpublished", required = false)
    var yearPublished: BggValueInt? = null
)