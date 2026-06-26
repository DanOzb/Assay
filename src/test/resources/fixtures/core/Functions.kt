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

//DOCS

// line comment
fun docs_none(x: Int): Int {
    return x
}

/* regular block comment */
fun docs_blockCommentNotDoc(x: Int): Int {
    return x
}

/** single line docs */
fun docs_simple(x: Int): Int = x

/**
 * multiline docs.
 *
 * Detailed description paragraph that should be part of the doc body
 * but is separate from the summary.
 */
fun docs_multiline(x: Int): Int = x

//ANNOTATIONS

fun anno_none(x: Int): Int = x

@Pure
fun anno_marker(x: Int): Int = x

@Suppress("unused")
fun anno_positional(x: Int): Int = x

//FUNCTION BODY

fun body_block(a: Int, b: Int): Int {
    val sum = a + b
    return sum
}

fun body_expression(a: Int, b: Int): Int = a + b

//SUSPEND

suspend fun is_suspend(s: String): String

fun is_not_suspend(s: String): String

