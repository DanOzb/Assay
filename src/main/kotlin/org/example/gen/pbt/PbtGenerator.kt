package org.example.gen.pbt

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.example.core.ParsedFunction
import org.example.gen.clients.OllamaClient
import org.example.gen.pbt.models.Decision
import org.example.gen.pbt.models.InvariantPlan
import org.example.gen.pbt.models.LlmRequest
import org.example.gen.pbt.prompts.InvariantBuildPrompt
import org.example.gen.pbt.testGen.generateTestClass
import org.example.gen.utils.decodeFromMessyContent

class PbtGenerator {

    private val lenientJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun run(functions: List<ParsedFunction>) {
        val invPlans: Map<ParsedFunction, InvariantPlan> = generateInvariants(functions)
        generateTests(invPlans)
    }

    private fun generateInvariants(functions: List<ParsedFunction>): Map<ParsedFunction, InvariantPlan> {
        val result = mutableMapOf<ParsedFunction, InvariantPlan>()
        val mutex = Mutex()
        runBlocking {
            OllamaClient().use { client ->
                for (fn in functions) {
                    println("STARTING INVARIANT GENERATION FOR FUNCTION ${fn.name}")

                    val params = fn.formatParams()
                    val receiver = fn.formatReceiverPrefix()

                    val prompt = InvariantBuildPrompt()

                    val response = client.complete(
                        LlmRequest(
                            messages = listOf(
                                prompt.getSystemMessage(),
                                prompt.getUserMessage(fn, params, receiver),
                            ),
                            jsonSchema = null,
                            think = true,
                            numCtx = 8192,
                            temperature = 0.5
                        )
                    )

                    val plan = lenientJson.decodeFromMessyContent<InvariantPlan>(response.content)
                    if (plan != null) {
                        mutex.withLock { result[fn] = plan }
                        println(plan.invariants)
                    } else {
                        println("FUNCTION ${fn.name} IS NULL")
                    }
                }
            }
        }
        return result
    }

    fun generateTests(invPlans: Map<ParsedFunction, InvariantPlan>) {
        println("Generating tests...")
        for ((fn, invPlan) in invPlans) {
            if (invPlan.decision == Decision.SKIP) {
                println("// SKIP ${fn.fullName}: ${invPlan.skipReason ?: "no reason given"}")
                continue
            }
            val rendered = generateTestClass(fn, invPlan)
            if (rendered.isNotBlank()) println(rendered)
        }
    }
}

fun ParsedFunction.formatParams(): String =
    if (params.isEmpty()) "none"
    else params.joinToString(", ") { "${it.name}: ${it.type}" }

fun ParsedFunction.formatReceiverPrefix(): String =
    receiver?.let { "$it." } ?: ""