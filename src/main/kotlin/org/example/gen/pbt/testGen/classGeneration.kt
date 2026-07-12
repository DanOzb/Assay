package org.example.gen.pbt.testGen

import org.example.core.ParsedFunction
import org.example.gen.pbt.models.InvariantPlan

fun generateTestClass(fn: ParsedFunction, invPlan: InvariantPlan): String {
    val invariants = invPlan.invariants?.takeIf { it.isNotEmpty() } ?: return ""

    val tests = invariants.joinToString("\n\n") { generateTestFunction(fn, it) }
    val pkg = fn.packageName

    return buildString {
        if (pkg.isNotEmpty()) {
            appendLine("package $pkg")
            appendLine()
        }
        appendLine(KOTEST_IMPORTS)
        appendLine()
        appendLine("class ${testClassName(fn)} : FunSpec({")
        appendLine(tests.prependIndent("    "))
        append("})")
    }
}

private fun testClassName(fn: ParsedFunction): String =
    fn.name.replaceFirstChar { it.uppercase() } + "Test"