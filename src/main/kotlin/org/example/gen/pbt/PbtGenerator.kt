package org.example.gen.pbt

import kotlinx.coroutines.runBlocking
import org.example.core.ParsedFunction
import org.example.gen.clients.OllamaClient
import kotlin.use

class PbtGenerator(
    private val functions: List<ParsedFunction>
) {

    fun run() {
        generateInvariants()
        //TODO: generate tests from invariants
    }

    fun generateInvariants(): List<LlmResponse> {
        val result = mutableListOf<LlmResponse>()
        runBlocking {
            OllamaClient().use { client ->
                for (fn in functions) {
                    val params = fn.formatParams()
                    val receiver = fn.formatReceiverPrefix()

                    val prompt = PbtPrompt()

                    val response = client.complete(
                        LlmRequest(
                            messages = listOf(
                                prompt.getSystemMessage(),
                                prompt.getUserMessage(fn, params, receiver),
                            ),
                            jsonSchema = prompt.getSchema(),
                            temperature = 0.1,
                        )
                    )
                    result.add(response)
                    println("For function: " + fn.name)
                    println(response.content)
                }
            }
        }
        return result
    }
}
fun ParsedFunction.formatParams(): String =
    if (params.isEmpty()) "none"
    else params.joinToString(", ") { "${it.name}: ${it.type}" }

fun ParsedFunction.formatReceiverPrefix(): String =
    receiver?.let { "$it." } ?: ""
