package org.example.gen.pbt.testGen

import org.example.core.ParsedFunction
import org.example.gen.pbt.models.Bound
import org.example.gen.pbt.models.Invariant
import org.example.gen.pbt.models.Rendered
import org.example.gen.pbt.models.TypedSlot

private const val RESULT_NAME = "result"


/**
 * Renders the Kotest assertion/s for one [invariant] over [fn].
 *
 * @param fn a parsed function
 * @param invariant the invariant picked from the llm
 * @return [Rendered] if invariant could be rendered, null otherwise.
 */
fun renderInvariant(fn: ParsedFunction, invariant: Invariant): Rendered? {
    return when (invariant.kind) {
        "involution", "idempotent" -> renderInvolutionOrIdempotent(fn, invariant)
        "commutative" -> renderCommutative(fn)
        "associative" -> renderAssociative(fn)
        "identity_element" -> renderIdentityElement(fn, invariant)
        "length_preserving" -> renderLengthPreserving(fn, invariant)
        "permutation_invariant" -> renderPermutationInvariant(fn, invariant)
        "never_throws" -> renderNeverThrows(fn)
        "output_constraint" -> invariant.predicate?.let { renderWithResult(fn, "(${invariant.predicate}) shouldBe true") }
        "oracle" -> invariant.reference?.let { renderWithResult(fn, "$RESULT_NAME shouldBe $it") }
        "custom" -> invariant.code?.let {renderWithResult(fn, it)}
        else -> null
    }
}

private fun renderInvolutionOrIdempotent(fn: ParsedFunction, invariant: Invariant): Rendered? {
    val bound = signatureBounds(fn)?.singleOrNull() ?: return null
    val once = callExpr(fn, listOf(bound.name))
    val rhs = if(invariant.kind == "involution") bound.name else once
    return Rendered(listOf(bound), listOf("${callExpr(fn, listOf(once))} shouldBe $rhs"))
}

private fun renderCommutative(fn: ParsedFunction): Rendered? {
    val ops = binaryOperandSlots(fn)?.let(::boundsFor) ?: return null
    val (a, c) = ops.map { it.name }
    return Rendered(ops, listOf("${callExpr(fn, listOf(a, c))} shouldBe ${callExpr(fn, listOf(c, a))}"))
}

private fun renderAssociative(fn: ParsedFunction): Rendered? {
    val slots = binaryOperandSlots(fn) ?: return null
    val ops = boundsFor(slots) ?: return null
    val third = freshName(ops.map { it.name }, "c")
    val thirdArb = arbForType(slots.first().type) ?: return null
    val bounds = ops + Bound(third, thirdArb)
    val (a, b) = ops.map { it.name }
    val left = callExpr(fn, listOf(callExpr(fn, listOf(a, b)), third))
    val right = callExpr(fn, listOf(a, callExpr(fn, listOf(b, third))))
    return Rendered(bounds, listOf("$left shouldBe $right"))
}

private fun renderIdentityElement(fn: ParsedFunction, invariant: Invariant): Rendered? {
    val inv = invariant.value ?: return null
    val slots = binaryOperandSlots(fn) ?: return null
    val operand = slots.first()
    val arb = arbForType(operand.type) ?: return null
    val operandName = operand.name
    return Rendered(
        listOf(Bound(operandName, arb)),
        listOf(
            "${callExpr(fn, listOf(operandName, inv))} shouldBe $operandName",
            "${callExpr(fn, listOf(inv, operandName))} shouldBe $operandName",
        ),
    )
}

private fun renderLengthPreserving(fn: ParsedFunction, invariant: Invariant): Rendered? {
    val b = signatureBounds(fn) ?: return null
    if (!hasSize(fn.returnType)) return null
    val slot = resolveHintedSlot(fn, invariant.args.firstOrNull()) { hasSize(it.type) } ?: return null
    val outputSize = withSizeAccess(callExpr(fn, b.map { it.name }), fn.returnType)
    val inputSize = withSizeAccess(slot.name, slot.type)
    return Rendered(b, listOf("$outputSize shouldBe $inputSize"))
}

private fun renderPermutationInvariant(fn: ParsedFunction, invariant: Invariant): Rendered? {
    val b = signatureBounds(fn) ?: return null
    val slot = resolveHintedSlot(fn, invariant.args.firstOrNull()) { isListType(it.type) }
        ?: return null
    val names = b.map { it.name }
    val shuffled = names.map { if (it == slot.name) "$it.shuffled()" else it }
    return Rendered(b, listOf("${callExpr(fn, names)} shouldBe ${callExpr(fn, shuffled)}"))
}

private fun renderNeverThrows(fn: ParsedFunction): Rendered? {
    val b = signatureBounds(fn) ?: return null
    return Rendered(b, listOf("shouldNotThrowAny { ${callExpr(fn, b.map { it.name })} }"))
}

private fun renderWithResult(fn: ParsedFunction, assertion: String): Rendered? {
    val bounds = signatureBounds(fn) ?: return null
    if (bounds.any { it.name == RESULT_NAME }) return null
    val call = callExpr(fn, bounds.map { it.name })
    return Rendered(bounds, listOf("val $RESULT_NAME = $call", assertion))
}

/**
 *
 * helper function for length preserving rendering.
 * @return length or size access with correct syntax.
 *
 */
private fun withSizeAccess(expr: String, type: String): String {
    val op = if (type.trim().endsWith("?")) "?." else "."
    val member = if(baseTypeName(type) in setOf("String", "CharSequence")) "length" else "size"
    return "$expr$op$member"
}

/**
 *
 * helper function that returns correct type name.
 *
 */
private fun baseTypeName(type: String): String =
    type.substringBefore('<').removeSuffix("?").trim()

private fun hasSize(type: String): Boolean = baseTypeName(type) in SIZED_TYPES
private fun isListType(type: String): Boolean = baseTypeName(type) in LIST_TYPES

/**
 * Maps each slot to a [Bound] pairing its name with an Arb for its type.
 *
 * @param slots the typed slots to bind, in order.
 * @return one [Bound] per slot, or `null` if any type is unsupported.
 */
private fun boundsFor(slots: List<TypedSlot>): List<Bound>? = slots.map {
    Bound(it.name, arbForType(it.type) ?: return null)
}

/**
 * Wrapper over [boundsFor]
 *
 * @return one [Bound] per signature slot, or `null` if any parameter or the
 * receiver has a type with no known Arb.
 */
private fun signatureBounds(fn: ParsedFunction) = boundsFor(signatureSlots(fn))
private fun callExpr(fn: ParsedFunction, args: List<String>): String =
    if (fn.receiver != null)
        "${args.first()}.${fn.name}(${args.drop(1).joinToString(", ")})"
    else
        "${fn.name}(${args.joinToString(", ")})"

private fun freshName(existing: List<String>, base: String): String {
    if (base !in existing) return base
    var i = 2
    while ("$base$i" in existing) i++
    return "$base$i"
}

private fun binaryOperandSlots(fn: ParsedFunction): List<TypedSlot>? {
    val slots = signatureSlots(fn)
    if (slots.size != 2) return null
    return if (slots[0].type.trim() == slots[1].type.trim()) slots else null
}

private fun resolveHintedSlot(
    fn: ParsedFunction,
    hint: String?,
    predicate: (TypedSlot) -> Boolean
): TypedSlot? {
    val candidates = signatureSlots(fn).filter(predicate)
    val hinted = hint?.let { h -> candidates.find { it.name == h } }
    return hinted ?: candidates.singleOrNull()
}

/**
 *
 * Generates correct arb from parameter type.
 * works recursively for list, map and triple types
 * Also checks for nullable types
 *
 * @param type type of the parameter
 * @return arb for the parameters type
 *
 */
fun arbForType(type: String): String? {
    val t = type.trim()
    if (t.endsWith("?"))
        return arbForType(t.removeSuffix("?").trim())?.let { "$it.orNull()" }

    val name = t.substringBefore('<').trim()
    val args = typeArgs(t)

    return when (name) {
        "Int" -> "Arb.int()"
        "Long" -> "Arb.long()"
        "Short" -> "Arb.short()"
        "Byte" -> "Arb.byte()"
        "Float" -> "Arb.float()"
        "Double" -> "Arb.double()"
        "Boolean" -> "Arb.boolean()"
        "Char" -> "Arb.char()"
        "String" -> "Arb.string()"
        
        "IntArray" -> "Arb.list(Arb.int()).map { it.toIntArray() }"
        "LongArray" -> "Arb.list(Arb.long()).map { it.toLongArray() }"
        "ShortArray" -> "Arb.list(Arb.short()).map { it.toShortArray() }"
        "ByteArray" -> "Arb.list(Arb.byte()).map { it.toByteArray() }"
        "CharArray" -> "Arb.list(Arb.char()).map { it.toCharArray() }"
        "FloatArray" -> "Arb.list(Arb.float()).map { it.toFloatArray() }"
        "DoubleArray" -> "Arb.list(Arb.double()).map { it.toDoubleArray() }"
        "BooleanArray" -> "Arb.list(Arb.boolean()).map { it.toBooleanArray() }"

        "List", "MutableList", "Collection", "Iterable" ->
            args.singleOrNull()?.let { arbForType(it) }?.let { "Arb.list($it)" }
        "Set", "MutableSet" ->
            args.singleOrNull()?.let { arbForType(it) }?.let { "Arb.set($it)" }
        "Array" ->
            args.singleOrNull()?.let { arbForType(it) }?.let { "Arb.list($it).map { it.toTypedArray() }" }

        "Map", "MutableMap" -> {
            val (k, v) = args.takeIf { it.size == 2 } ?: return null
            "Arb.map(${arbForType(k) ?: return null}, ${arbForType(v) ?: return null})"
        }
        "Pair" -> {
            val (a, b) = args.takeIf { it.size == 2 } ?: return null
            "Arb.pair(${arbForType(a) ?: return null}, ${arbForType(b) ?: return null})"
        }
        "Triple" -> {
            val parts = args.takeIf { it.size == 3 } ?: return null
            val arbs = parts.map { arbForType(it) ?: return null }
            "Arb.triple(${arbs.joinToString(", ")})"
        }

        else -> null
    }
}

/**
 *
 * checks if parameter type has arguments such as List<Int>.
 * returns types in a list of strings
 *
 * @param type type of the parameter
 * @return list of type arguments or empty list if none
 *
 */

private fun typeArgs(type: String): List<String> {
    val open = type.indexOf('<')
    if (open == -1) return emptyList()
    val close = type.lastIndexOf('>')
    if (close <= open) return emptyList()

    val inner = type.substring(open + 1, close)
    val out = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in inner.indices) {
        when (inner[i]) {
            '<' -> depth++
            '>' -> depth--
            ',' -> if (depth == 0) { out += inner.substring(start, i).trim(); start = i + 1 }
        }
    }
    out += inner.substring(start).trim()
    return out.filter { it.isNotEmpty() }
}
