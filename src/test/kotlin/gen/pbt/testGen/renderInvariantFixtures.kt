package org.example.gen.pbt.testGen

import org.example.core.ParsedFunction
import org.example.core.ParsedParam
import org.example.gen.pbt.models.Invariant

/**
 * Shared fixtures for renderInvariant tests.
 *
 */


fun fn(
    name: String,
    params: List<ParsedParam> = emptyList(),
    returnType: String = "Unit",
    receiver: String? = null,
): ParsedFunction = ParsedFunction(
    name = name,
    fullName = "org.example.$name",
    receiver = receiver,
    params = params,
    returnType = returnType,
    visibility = "public",
    docs = null,
    annotations = emptyList(),
    body = null,
    isSuspend = false,
)

fun invariant(
    kind: String,
    testName: String = "prop_$kind",
    args: List<String> = emptyList(),
    value: String? = null,
    predicate: String? = null,
    reference: String? = null,
    code: String? = null,
): Invariant = Invariant(
    kind = kind,
    testName = testName,
    args = args,
    value = value,
    predicate = predicate,
    reference = reference,
    code = code,
)


object Fns {

    /** Single arb-able param. */
    val negate = fn("negate", listOf(ParsedParam("x", "Int")), "Int")
    val abs = fn("abs", listOf(ParsedParam("x", "Int")), "Int")

    /** Single slot via a receiver, no params -> exercises callExpr's receiver branch. */
    val stringReversed = fn("reversed", returnType = "String", receiver = "String")

    /** Single param whose type has no known arb -> arbForType returns null. */
    val negateWidget = fn("negate", listOf(ParsedParam("w", "Widget")), "Widget")

    /** Zero slots -> b.size != 1. */
    val now = fn("now", returnType = "Long")

    val add = fn("add", listOf(ParsedParam("a", "Int"), ParsedParam("b", "Int")), "Int")

    /** Binary via receiver + one param of the same type. */
    val intPlus = fn("plus", listOf(ParsedParam("other", "Int")), "Int", receiver = "Int")

    /** Binary, same type, params already named c/d -> associative must pick a fresh "c2". */
    val combineCd = fn("combine", listOf(ParsedParam("c", "Int"), ParsedParam("d", "Int")), "Int")

    /** Two params, different types -> not a binary operand pair. */
    val repeatStr = fn("repeat", listOf(ParsedParam("s", "String"), ParsedParam("n", "Int")), "String")

    /** Two params, same type, but the type has no arb. */
    val mergeWidget = fn("merge", listOf(ParsedParam("a", "Widget"), ParsedParam("b", "Widget")), "Widget")

    val reverseList = fn("reverse", listOf(ParsedParam("xs", "List<Int>")), "List<Int>")
    val sortList = fn("sort", listOf(ParsedParam("xs", "List<Int>")), "List<Int>")

    /** String in / String out -> sizeAccessor should pick `.length`, not `.size`. */
    val reverseString = fn("reverseString", listOf(ParsedParam("s", "String")), "String")

    /** Two list params -> ambiguous for resolveHintedSlot (needs an args hint). */
    val zip = fn(
        "zip",
        listOf(ParsedParam("a", "List<Int>"), ParsedParam("b", "List<Int>")),
        "List<Pair<Int, Int>>",
    )

    /** Sized param but non-sized return -> length_preserving should bail. */
    val count = fn("count", listOf(ParsedParam("xs", "List<Int>")), "Int")

    val parsePositive = fn("parsePositive", listOf(ParsedParam("s", "String")), "Int")

    val fill = fn("fill", listOf(ParsedParam("n", "Int")), "List<Int>")
    val dropFirst = fn("dropFirst", returnType = "List<Int>", receiver = "List<Int>")

    /** Nullable return -> output accessor must be `f(xs)?.size`. */
    val reverseListNullableOut = fn("reverse", listOf(ParsedParam("xs", "List<Int>")), "List<Int>?")

    /** Nullable list param -> arb is `.orNull()` and input accessor must be `xs?.size`. */
    val reverseListNullableIn = fn("reverse", listOf(ParsedParam("xs", "List<Int>?")), "List<Int>")

    /** Param literally named `result` -> oracle/output_constraint/custom must TODO (null). */
    val scoreResult = fn("score", listOf(ParsedParam("result", "Int")), "Int")

}

object Invs {

    val involution = invariant("involution")
    val idempotent = invariant("idempotent")

    val commutative = invariant("commutative")
    val associative = invariant("associative")

    val identityZero = invariant("identity_element", value = "0")
    val identityNoValue = invariant("identity_element") // value == null -> null

    val lengthPreserving = invariant("length_preserving")
    val lengthPreservingHinted = invariant("length_preserving", args = listOf("a"))
    val lengthPreservingHintB = invariant("length_preserving", args = listOf("b"))
    val lengthPreservingBadHint = invariant("length_preserving", args = listOf("zzz"))

    val permutationInvariant = invariant("permutation_invariant")
    val permutationInvariantHinted = invariant("permutation_invariant", args = listOf("a"))
    val permutationInvariantHintB = invariant("permutation_invariant", args = listOf("b"))

    val neverThrows = invariant("never_throws")

    val outputConstraint = invariant("output_constraint", predicate = "result >= 0")
    val outputConstraintNoPredicate = invariant("output_constraint") // predicate == null -> null

    val oracle = invariant("oracle", reference = "xs.sorted()")
    val oracleNoReference = invariant("oracle") // reference == null -> null

    val custom = invariant("custom", code = "result shouldHaveSize xs.size")
    val customNoCode = invariant("custom") // code == null -> null

    val unknownKind = invariant("teleporting") // else branch -> null
}