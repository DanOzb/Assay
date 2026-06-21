package core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.example.core.ParsedFunction
import org.example.core.Parser
import java.nio.file.Paths

class ParserTest : FunSpec({

    val parser = Parser()

    lateinit var sharedResults: List<ParsedFunction>

    beforeSpec {
        val resourceUri = this::class.java.getResource("/fixtures/core")?.toURI()
            ?: error("Fixture directory not found.")

        sharedResults = parser.inspectDir(Paths.get(resourceUri))
    }

    fun findFunction(name: String): ParsedFunction {
        return sharedResults.find { it.name == name }
            ?: error("Function '$name' was not found")
    }

    test("should parse a basic single-parameter function (reverse)") {
        val fn = findFunction("reverse")
        fn.returnType shouldBe "String"
        fn.visibility shouldBe "PUBLIC"
        fn.receiver.shouldBeNull()

        fn.params shouldHaveSize 1
        fn.params[0].name shouldBe "s"
        fn.params[0].type shouldBe "String"
    }

    test("should parse multiple parameters (add)") {
        val fn = findFunction("add")
        fn.returnType shouldBe "Int"

        fn.params shouldHaveSize 2
        fn.params[0].name shouldBe "a"
        fn.params[0].type shouldBe "Int"
        fn.params[1].name shouldBe "b"
        fn.params[1].type shouldBe "Int"
    }

    test("should handle parameterless functions (greet)") {
        val fn = findFunction("greet")
        fn.returnType shouldBe "String"
        fn.params.shouldHaveSize(0)
    }

    test("should resolve inferred return type (inferred)") {
        val fn = findFunction("inferred")
        fn.returnType shouldBe "Int"
    }

    test("should maintain nullability tokens on parameter types (nullableIn)") {
        val fn = findFunction("nullableIn")

        fn.params shouldHaveSize 1
        fn.params[0].name shouldBe "s"
        fn.params[0].type shouldBe "String?"
    }

    test("should extract the extension receiver type correctly (slugify)") {
        val fn = findFunction("slugify")
        fn.returnType shouldBe "String"

        fn.receiver.shouldNotBeNull()
        fn.receiver shouldBe "String"
    }

    test("should correctly recognize internal visibility modifiers (secret)") {
        val fn = findFunction("secret")
        fn.visibility shouldBe "INTERNAL"
    }

    test("should correctly recognize private visibility modifiers (hidden)") {
        val fn = findFunction("hidden")
        fn.visibility shouldBe "PRIVATE"
        fn.returnType shouldBe "Boolean"
    }

    test("parses exactly the expected top-level functions") {
        sharedResults.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "reverse", "add", "greet", "inferred", "nullableIn", "slugify", "secret", "hidden",
        )
    }

    test("parses fullName correctly") {
        findFunction("reverse").fullName shouldBe "core.reverse"
    }
})