package org.example.gen.pbt.testGen

import org.example.core.ParsedFunction
import org.example.gen.pbt.models.TypedSlot

private const val RECEIVER_NAME = "receiver"

val LIST_TYPES = setOf("List", "MutableList", "Collection", "Iterable")
val SIZED_TYPES = LIST_TYPES + setOf(
    "String",
    "CharSequence",
    "Set",
    "MutableSet",
    "Array",
    "Map",
    "MutableMap",
    "IntArray",
    "LongArray",
    "ShortArray",
    "ByteArray",
    "CharArray",
    "FloatArray",
    "DoubleArray",
    "BooleanArray",)

val KOTEST_IMPORTS = listOf(
    "import io.kotest.core.spec.style.FunSpec",
    "import io.kotest.matchers.shouldBe",
    "import io.kotest.assertions.throwables.shouldNotThrowAny",
    "import io.kotest.property.Arb",
    "import io.kotest.property.assume",
    "import io.kotest.property.checkAll",
    "import io.kotest.property.arbitrary.*",
).joinToString("\n")

fun signatureSlots(fn: ParsedFunction): List<TypedSlot> =
    (if (fn.receiver != null)
        listOf(TypedSlot(name = RECEIVER_NAME, type = fn.receiver))
    else
        emptyList()) + fn.params.map { TypedSlot(it.name, it.type) }

