package com.soywiz.korge.intellij.internal

fun String.escape(): String {
    val out = StringBuilder()
    for (n in 0 until this.length) {
        val c = this[n]
        when (c) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            in '\u0000'..'\u001f' -> {
                out.append("\\x")
                out.append(Hex.encodeCharLower(c.toInt().extract(4, 4)))
                out.append(Hex.encodeCharLower(c.toInt().extract(0, 4)))
            }
            else -> out.append(c)
        }
    }
    return out.toString()
}

fun String.uescape(): String {
    val out = StringBuilder()
    for (n in 0 until this.length) {
        val c = this[n]
        when (c) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> if (c.isPrintable()) {
                out.append(c)
            } else {
                out.append("\\u")
                out.append(Hex.encodeCharLower(c.toInt().extract(12, 4)))
                out.append(Hex.encodeCharLower(c.toInt().extract(8, 4)))
                out.append(Hex.encodeCharLower(c.toInt().extract(4, 4)))
                out.append(Hex.encodeCharLower(c.toInt().extract(0, 4)))
            }
        }
    }
    return out.toString()
}

fun String.unescape(): String {
    val out = StringBuilder()
    var n = 0
    while (n < this.length) {
        val c = this[n++]
        when (c) {
            '\\' -> {
                val c2 = this[n++]
                when (c2) {
                    '\\' -> out.append('\\')
                    '"' -> out.append('\"')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> {
                        val chars = this.substring(n, n + 4)
                        n += 4
                        out.append(chars.toInt(16).toChar())
                    }
                    else -> {
                        out.append("\\$c2")
                    }
                }
            }
            else -> out.append(c)
        }
    }
    return out.toString()
}

fun String?.uquote(): String = if (this != null) "\"${this.uescape()}\"" else "null"
fun String?.quote(): String = if (this != null) "\"${this.escape()}\"" else "null"

fun String.isQuoted(): Boolean = this.startsWith('"') && this.endsWith('"')
fun String.unquote(): String = if (isQuoted()) this.substring(1, this.length - 1).unescape() else this

val String?.quoted: String get() = this.quote()
val String.unquoted: String get() = this.unquote()

object Hex {
    private const val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.toUpperCase()
    val DIGITS_LOWER = DIGITS.toLowerCase()
    fun encodeCharLower(v: Int): Char = DIGITS_LOWER[v]
}

private fun Char.isPrintable(): Boolean = this in '\u0020'..'\u007e' || this in '\u00a1'..'\u00ff'

private fun Int.mask(): Int = (1 shl this) - 1
private fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
