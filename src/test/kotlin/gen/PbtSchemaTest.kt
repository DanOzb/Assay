package gen

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.gen.pbt.PbtPrompt

/**
 *
 * Test cases incase I change schema later
 *
 */

class PbtSchemaTest: FunSpec( {

    fun JsonObject.at(vararg path: String): JsonObject =
        path.fold(this) { obj, key ->
            obj[key]?.jsonObject ?: error("missing object at key '$key'")
        }

    fun JsonObject.enumValues(): List<String> =
        this["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: error("expected an 'enum' array here")

    val schema = PbtPrompt().getSchema()

    test("schema requires a decision field"){
        val required = schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        required shouldContain "decision"
    }

    test("decision offers exactly generate and skip"){
        val decision = schema.at("properties", "decision").enumValues()
        decision shouldContainAll listOf("generate", "skip")
    }

    test("kind catalog contains every invariant the prompt relies on"){
        val kinds = schema.at("properties", "invariants", "items", "properties", "kind").enumValues()
        kinds shouldContainAll listOf(
            "involution", "idempotent", "commutative", "associative",
            "identity_element", "length_preserving", "permutation_invariant",
            "never_throws", "output_constraint", "oracle", "custom",
        )
    }
})