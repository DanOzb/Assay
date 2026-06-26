package gen

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.example.core.ParsedFunction
import org.example.core.ParsedParam
import org.example.gen.pbt.formatParams
import org.example.gen.pbt.formatReceiverPrefix

class PbtGeneratorTest: FunSpec({

    val empty = ParsedFunction(
        params = emptyList(),
        name = "",
        fullName = "",
        receiver = null,
        returnType = "",
        visibility = "",
        docs = null,
        annotations = emptyList(),
        body = null,
        isSuspend = false,
    )

    val notEmpty = ParsedFunction(
        params = listOf(
            ParsedParam("a", "String"),
            ParsedParam("b", "String"),
        ),
        name = "",
        fullName = "",
        receiver = "String",
        returnType = "",
        visibility = "",
        docs = null,
        annotations = emptyList(),
        body = null,
        isSuspend = false,
    )

    test("format params renders 'none' when there is no params"){
        empty.formatParams() shouldBe "none"
    }

    test("format params renders 'name: type' comma separated, in order"){
        notEmpty.formatParams() shouldBe "a: String, b: String"
    }

    test("formatReceiverPrefix appends a dot for extension functions"){
        notEmpty.formatReceiverPrefix() shouldBe "String."
    }

    test("formatReceiverPrefix renders empty string for null receiver"){
        empty.formatReceiverPrefix() shouldBe ""
    }
})