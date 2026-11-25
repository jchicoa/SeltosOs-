package com.example.media

import com.example.model.MediaItemEntry
import com.example.model.MediaSourceType

/**
 * Parses standard M3U / M3U8 IPTV playlists into playable MediaItemEntry items.
 */
object IptvPlaylistParser {

    fun parseM3uContent(content: String): List<MediaItemEntry> {
        val entries = mutableListOf<MediaItemEntry>()
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var pendingTitle: String? = null
        var pendingGroup = "IPTV"

        for (line in lines) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val commaIdx = line.indexOf(',')
                pendingTitle = if (commaIdx != -1 && commaIdx + 1 < line.length) {
                    line.substring(commaIdx + 1).trim()
                } else {
                    "IPTV Channel ${entries.size + 1}"
                }
                val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
                pendingGroup = groupMatch?.groupValues?.getOrNull(1)?.ifBlank { "IPTV" } ?: "IPTV"
            } else if (!line.startsWith("#")) {
                val title = pendingTitle ?: "Stream ${entries.size + 1}"
                entries.add(
                    MediaItemEntry(
                        id = "iptv_${entries.size}_${line.hashCode()}",
                        title = title,
                        uri = line,
                        type = MediaSourceType.IPTV_HLS,
                        group = pendingGroup
                    )
                )
                pendingTitle = null
                pendingGroup = "IPTV"
            }
        }
        return entries
    }
}
