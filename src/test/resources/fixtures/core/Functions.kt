package core
/**
 *
 * Sample functions to test code parsing.
 * Only TOP_LEVEL functions
 *
 */

fun reverse(s: String): String = s
fun add(a: Int, b: Int): Int = a + b
fun greet(): String = "hi"
fun inferred() = 42
fun nullableIn(s: String?): Int = 0
fun String.slugify(): String = "x"

internal fun secret(n: Int): Int = n
private fun hidden(): Boolean = true