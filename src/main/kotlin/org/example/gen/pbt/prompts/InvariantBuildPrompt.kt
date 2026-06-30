package org.example.gen.pbt.prompts

import org.example.core.ParsedFunction
import org.example.gen.pbt.models.LlmMessage
import org.example.gen.pbt.models.Role

class InvariantBuildPrompt {
    fun getSystemMessage(): LlmMessage {
        return LlmMessage(
            role = Role.SYSTEM,
            content =
                """You are an expert Kotlin test engineer. Your job is to analyze a function and pick
                   invariants for property-based testing using its signature, KDoc, annotations, and body.

                   First, output your step-by-step reasoning about the function's properties.
                   Then, output your final decision in strict JSON format enclosed in ```json ``` blocks.
                   Worked examples for every invariant kind are provided below; follow their shape exactly.
                   """.trimIndent()
        )
    }

    fun getUserMessage(fn: ParsedFunction, params: String, receiver: String): LlmMessage {
        val kdoc: String = fn.docs ?: "none"
        val body: String = fn.body ?: "none"
        val annotations: String =
            if (fn.annotations.isEmpty()) "none"
            else fn.annotations.joinToString(", ") { a ->
                if (a.arguments.isEmpty()) a.name else "${a.name}(${a.arguments.joinToString(", ")})"
            }
        val prompt = """
        Decide which property-based test properties hold for this function.
        Function under test
          name:        ${fn.fullName}
          signature:   $receiver${fn.name}($params): ${fn.returnType}
          visibility:  ${fn.visibility}    suspend: ${fn.isSuspend}
          annotations: $annotations
          kdoc:        $kdoc
          body:        $body

        How inputs are named in your expressions:
        - Each parameter is referred to by its exact name from the signature.
        - If the function is an extension (has a receiver), the receiver is referred to as
          `receiver`. Use `receiver`, never the receiver's type name.
        - `result` is the value the function returns.

        Catalog of Invariants (★ = requires an extra field; see "Required fields"):
          involution             f(f(x)) == x                 single-input functions only
          idempotent             f(f(x)) == f(x)              single-input functions only
          commutative            f(a,b) == f(b,a)             two same-typed operands only
          associative            f(f(a,b),c) == f(a,f(b,c))   two same-typed operands only
          identity_element ★     f(x,e) == x                  two same-typed operands; value = e
          length_preserving      |out| == |in|                args = ["<collection input>"]
          permutation_invariant  order-independent            args = ["<list input>"]
          never_throws           total over all inputs
          output_constraint ★    predicate on result          predicate = <Bool over result/inputs>
          oracle ★               equals a reference           reference = <expr over inputs>
          custom ★               anything else                code = <assertion body>

        Which kinds take "args":
        - ONLY length_preserving and permutation_invariant take an "args" array, and it names
          the SINGLE collection input the property is about (e.g. ["xs"], or ["receiver"] if the
          collection is the extension receiver).
        - NO other kind takes "args". Do not emit "args" for any other kind.

        Required fields per ★ kind (a ★ invariant missing its field is INVALID and discarded —
        if you cannot write the field, pick a different kind or skip):
            identity_element   ->  "value":     e.g. "0", "1", "\"\""
            output_constraint  ->  "predicate": e.g. "result >= 0", "result.all { it >= 0 }"
            oracle             ->  "reference": e.g. "s.reversed()"
            custom             ->  "code":      e.g. "result shouldBe items.flatten()"

        JSON Format Requirements:
        - "decision": "generate" or "skip".
        - "skipReason": required when skip. Explains why no property holds.
        - "invariants": required when generate. Array of objects, each with "kind" and
          "testName", plus:
            * the required extra field for ★ kinds, and
            * "args" ONLY for length_preserving / permutation_invariant.
        - skip ONLY when NO property holds for ANY input, even under a precondition.

        Worked example for every kind (one invariant each):

          involution  ::  fun reverseString(s: String): String
        ```json
        { "kind": "involution", "testName": "reversing twice returns the original" }
        ```
          idempotent  ::  fun trimSpaces(s: String): String
        ```json
        { "kind": "idempotent", "testName": "trimming twice equals trimming once" }
        ```
          commutative  ::  fun gcd(a: Int, b: Int): Int
        ```json
        { "kind": "commutative", "testName": "gcd is commutative" }
        ```
          associative  ::  fun concat(a: String, b: String): String
        ```json
        { "kind": "associative", "testName": "concat is associative" }
        ```
          identity_element  ::  fun multiply(a: Int, b: Int): Int
        ```json
        { "kind": "identity_element", "testName": "1 is the identity for multiply", "value": "1" }
        ```
          length_preserving  ::  fun mapEncode(xs: List<Int>): List<Int>
        ```json
        { "kind": "length_preserving", "testName": "encoding preserves length", "args": ["xs"] }
        ```
          permutation_invariant  ::  fun sum(xs: List<Int>): Int
        ```json
        { "kind": "permutation_invariant", "testName": "sum is order-independent", "args": ["xs"] }
        ```
          never_throws  ::  fun parseOrZero(s: String): Int
        ```json
        { "kind": "never_throws", "testName": "parseOrZero never throws" }
        ```
          output_constraint  ::  fun abs(n: Int): Int
        ```json
        { "kind": "output_constraint", "testName": "abs is never negative", "predicate": "result >= 0" }
        ```
          oracle  ::  fun fastReverse(s: String): String
        ```json
        { "kind": "oracle", "testName": "fastReverse matches String.reversed", "reference": "s.reversed()" }
        ```
          custom  ::  fun flatten(items: List<List<Int>>): List<Int>
        ```json
        { "kind": "custom", "testName": "flatten concatenates all elements", "code": "result shouldBe items.flatten()" }
        ```

        Full output example (note: a function can satisfy several invariants, and no kind
        below carries "args"):

        I will analyze `abs(n: Int): Int`. It returns the magnitude of n, so the result is
        never negative — an output_constraint. Applying abs twice changes nothing, so it is
        also idempotent.

        ```json
        {
          "decision": "generate",
          "invariants": [
            {
              "kind": "output_constraint",
              "testName": "abs is never negative",
              "predicate": "result >= 0"
            },
            {
              "kind": "idempotent",
              "testName": "abs applied twice equals abs once"
            }
          ]
        }
        ```

        Analyze the provided function and output your reasoning followed by the JSON block.
    """.trimIndent()
        return LlmMessage(Role.USER, prompt)
    }
}