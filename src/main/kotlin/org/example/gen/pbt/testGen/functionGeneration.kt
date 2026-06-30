package org.example.gen.pbt.testGen

import org.example.core.ParsedFunction
import org.example.gen.pbt.models.Bound
import org.example.gen.pbt.models.Invariant
import org.example.gen.pbt.models.Precondition

fun generateTestFunction(fn: ParsedFunction, invariant: Invariant): String {
    val rendered = renderInvariant(fn, invariant)
        ?: return wrapTest(invariant.testName, listOf(todo(invariant)), emptyList())

    val fallback = signatureSlots(fn).firstOrNull()?.name ?: "it"
    val assumes = invariant.preconditions
        .map { renderPrecondition(it, fallback) }
        .filter { it.isNotBlank() }

    return wrapTest(invariant.testName, assumes + rendered.body, rendered.bounds)
}

private fun wrapTest(name: String, bodyLines: List<String>, bounds: List<Bound>): String {
    val body = bodyLines.joinToString("\n")
    return buildString {
        appendLine("test(\"$name\") {")
        if (bounds.isEmpty()) {
            appendLine(body.prependIndent("    "))
        } else {
            val arbs = bounds.joinToString(", ") { it.arb }
            val names = bounds.joinToString(", ") { it.name }
            appendLine("    checkAll($arbs) { $names ->")
            appendLine(body.prependIndent("        "))
            appendLine("    }")
        }
        append("}")
    }
}

private fun renderPrecondition(pc: Precondition, fallback: String): String = when (pc.kind) {
    "nonZero" -> "assume(${pc.args.firstOrNull() ?: fallback} != 0)"
    "sorted", "ordered" -> {
        val a = pc.args.firstOrNull() ?: fallback
        "assume($a == $a.sorted())"
    }
    "custom" -> "assume(${pc.predicate ?: "true"})"
    else -> ""
}

private fun todo(invariant: Invariant): String =
    "// TODO: unsupported or incomplete invariant '${invariant.kind}' (${invariant.testName})"