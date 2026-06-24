package org.example.gen.pbt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.example.Assay
import org.example.core.ParsedFunction

class PbtPrompt {

    fun getSystemMessage(): LlmMessage {
        return LlmMessage(
            role = Role.SYSTEM,
            content =
                """You are a senior Kotlin test engineer specialising in property-based testing with Kotest.
                    You reason about which mathematical/behavioural properties a function satisfies and report
                    them as structured data. You do NOT write test code. You only ever output JSON that matches
                    the requested schema — no prose, no markdown fences.
                    """.trimIndent()
        )
    }

    fun getUserMessage(fn: ParsedFunction, params: String, receiver: String): LlmMessage {
        return LlmMessage(
            role = Role.USER,
            """Decide which property-based test properties hold for this Kotlin function.
                
                    Function under test
                      fully-qualified name: ${fn.fullName}
                      signature:            $receiver${fn.name}($params): ${fn.returnType}
                      visibility:           ${fn.visibility}

                    Pick zero or more invariants from this catalog. For each, fill ONLY the
                    fields that invariant uses; leave the rest out.

                      involution            f(f(x)) == x.                       args=[the round-tripping parameter]
                      idempotent            f(f(x)) == f(x).                    args=[parameter]
                      commutative           f(a,b) == f(b,a).                   args=[two parameters]
                      associative           f(f(a,b),c) == f(a,f(b,c)).         args=[three parameters]
                      identity_element      f(x,e) == x for identity e.         args=[parameter], value=[e as Kotlin source]
                      length_preserving     output size/length == input's.      args=[the collection/String parameter]
                      permutation_invariant result unchanged if input shuffled. args=[the collection parameter]
                      never_throws          returns normally for all inputs.    args=[]  (all params are exercised)
                      output_constraint     a predicate on the result holds.    predicate=[Kotlin Boolean over `result` and params]
                      oracle                f == an independent reference.       reference=[Kotlin expr over params giving expected value]
                      custom                anything not above.                  code=[Kotlin assertion body; params and `result` in scope]

                    Rules
                      - Prefer catalog kinds over 'custom'.
                      - `args` must be EXACT parameter names from the signature, in order.
                      - Give every invariant a short human `testName`, e.g. "reverse is its own inverse".
                      - decision="generate" with a non-empty invariants array, OR
                        decision="skip" with a skipReason.
                      - Only skip when NO general property holds for ANY input. A function being
                        small or simple is not a reason to skip; find the property that holds.

                    Return ONLY JSON matching the provided schema. No prose, no code fences.
                    """.trimIndent()
        )
    }

    fun getSchema(): JsonObject {
        return Json.parseToJsonElement(
            Assay::class.java.getResource("/schema/pbt-test.schema.json")!!.readText()
        ).jsonObject
    }

}