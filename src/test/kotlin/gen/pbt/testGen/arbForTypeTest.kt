package gen.pbt.testGen

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.example.gen.pbt.testGen.arbForType

class ArbForTypeTest : FunSpec({

    context("collections") {
        val cases = listOf(
            "List<Int>" to "Arb.list(Arb.int())",
            "MutableList<String>" to "Arb.list(Arb.string())",
            "Collection<Int>" to "Arb.list(Arb.int())",
            "Iterable<Int>" to "Arb.list(Arb.int())",
            "Set<String>" to "Arb.set(Arb.string())",
            "MutableSet<Int>" to "Arb.set(Arb.int())",
            "Array<Int>" to "Arb.list(Arb.int()).map { it.toTypedArray() }",
        )
        cases.forEach { (type, expected) ->
            test("$type -> $expected") { arbForType(type) shouldBe expected }
        }
    }

    context("map, pair, triple") {
        val cases = listOf(
            "Map<String, Int>" to "Arb.map(Arb.string(), Arb.int())",
            "MutableMap<Int, String>" to "Arb.map(Arb.int(), Arb.string())",
            "Pair<Int, String>" to "Arb.pair(Arb.int(), Arb.string())",
            "Triple<Int, Long, String>" to "Arb.triple(Arb.int(), Arb.long(), Arb.string())",
        )
        cases.forEach { (type, expected) ->
            test("$type -> $expected") { arbForType(type) shouldBe expected }
        }
    }

    context("nesting") {
        val cases = listOf(
            "List<List<Int>>" to "Arb.list(Arb.list(Arb.int()))",
            "Map<String, List<Int>>" to "Arb.map(Arb.string(), Arb.list(Arb.int()))",
            "List<Pair<Int, Int>>" to "Arb.list(Arb.pair(Arb.int(), Arb.int()))",
            "Map<Int, Pair<String, Boolean>>" to "Arb.map(Arb.int(), Arb.pair(Arb.string(), Arb.boolean()))",
        )
        cases.forEach { (type, expected) ->
            test("$type -> $expected") { arbForType(type) shouldBe expected }
        }
    }

    context("nullability") {
        val cases = listOf(
            "Int?" to "Arb.int().orNull()",
            "String?" to "Arb.string().orNull()",
            "List<Int>?" to "Arb.list(Arb.int()).orNull()",
            "List<Int?>" to "Arb.list(Arb.int().orNull())",
            "Map<String, Int?>" to "Arb.map(Arb.string(), Arb.int().orNull())",
        )
        cases.forEach { (type, expected) ->
            test("$type -> $expected") { arbForType(type) shouldBe expected }
        }
    }

    context("unsupported -> null") {
        val types = listOf(
            "Widget",
            "IntArray",
            "List<Widget>",
            "Map<String, Widget>",
            "Set<Widget>",
        )
        types.forEach { type ->
            test("$type -> null") { arbForType(type).shouldBeNull() }
        }
    }

    context("wrong number of type arguments -> null") {
        val types = listOf(
            "Pair<Int>",
            "Triple<Int, Int>",
            "Map<Int>",
            "List<Int, Int>",
        )
        types.forEach { type ->
            test("$type -> null") { arbForType(type).shouldBeNull() }
        }
    }
})