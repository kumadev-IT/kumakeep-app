package com.kumadev.kumakeep.data.remote.dto

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "items", strict = false)
data class BggThingResponse(
    @field:ElementList(inline = true, required = false)
    var items: MutableList<BggItemDto> = mutableListOf()
)

@Root(name = "item", strict = false)
data class BggItemDto(
    @field:Attribute(name = "id", required = false)
    var id: Long = 0,

    @field:ElementList(inline = true, required = false, entry = "name")
    var names: MutableList<BggNameDto> = mutableListOf(),

    @field:Element(name = "yearpublished", required = false)
    var yearPublished: BggValueInt? = null,

    @field:Element(name = "minplayers", required = false)
    var minPlayers: BggValueInt? = null,

    @field:Element(name = "maxplayers", required = false)
    var maxPlayers: BggValueInt? = null,

    @field:Element(name = "playingtime", required = false)
    var playingTime: BggValueInt? = null,

    @field:Element(name = "minage", required = false)
    var minAge: BggValueInt? = null,

    @field:Element(name = "description", required = false)
    var description: String? = null,

    @field:Element(name = "thumbnail", required = false)
    var thumbnail: String? = null,

    @field:Element(name = "image", required = false)
    var image: String? = null,

    @field:ElementList(inline = true, required = false, entry = "link")
    var links: List<BggLinkDto> = mutableListOf(),

    @field:Element(name = "statistics", required = false)
    var statistics: BggStatisticsDto? = null
) {
    // helpers per estrarre le liste per tipo
    fun designers() = links.filter { it.type == "boardgamedesigner" }.map { it.value }
    fun artists() = links.filter { it.type == "boardgameartist" }.map { it.value }
    fun publishers() = links.filter { it.type == "boardgamepublisher" }.map { it.value }
    fun categories() = links.filter { it.type == "boardgamecategory" }.map { it.value }
    fun mechanics() = links.filter { it.type == "boardgamemechanic" }.map { it.value }
    fun families() = links.filter { it.type == "boardgamefamily" }.map { it.value }
    fun primaryName() = names.firstOrNull { it.type == "primary" }?.value ?: ""
}

@Root(name = "name", strict = false)
data class BggNameDto(
    @field:Attribute(name = "type", required = false)
    var type: String = "",

    @field:Attribute(name = "value", required = false)
    var value: String = ""
)

@Root(name = "link", strict = false)
data class BggLinkDto(
    @field:Attribute(name = "type", required = false)
    var type: String = "",

    @field:Attribute(name = "value", required = false)
    var value: String = ""
)

@Root(name = "statistics", strict = false)
data class BggStatisticsDto(
    @field:Element(name = "ratings", required = false)
    var ratings: BggRatingsDto? = null
)

@Root(name = "ratings", strict = false)
data class BggRatingsDto(
    @field:Element(name = "average", required = false)
    var average: BggValueFloat? = null,

    @field:Element(name = "averageweight", required = false)
    var averageWeight: BggValueFloat? = null
)

@Root(name = "value", strict = false)
data class BggValueInt(
    @field:Attribute(name = "value", required = false)
    var value: Int = 0
)

@Root(name = "value", strict = false)
data class BggValueFloat(
    @field:Attribute(name = "value", required = false)
    var value: Float = 0f
)