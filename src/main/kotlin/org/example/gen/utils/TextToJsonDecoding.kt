package org.example.gen.utils

import kotlinx.serialization.json.Json

fun extractJsonBlocks(raw: String): List<String> {
    val blocks = mutableListOf<String>()
    var i = 0
    while (i < raw.length) {
        if (raw[i] == '{') {
            val end = matchingBrace(raw, i)
            if (end != -1) {
                blocks += raw.substring(i, end + 1)
                i = end + 1
                continue
            }
        }
        i++
    }
    return blocks
}

private fun matchingBrace(s: String, start: Int): Int {
    var depth = 0
    var inString = false
    var escaped = false
    var i = start
    while (i < s.length) {
        val c = s[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
        } else {
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
        }
        i++
    }
    return -1
}

inline fun <reified T> Json.decodeFromMessyContent(raw: String): T? {
    for (block in extractJsonBlocks(raw).asReversed()) {
        try {
            return decodeFromString<T>(block)
        } catch (_: Exception) {
        }
    }
    return null
}