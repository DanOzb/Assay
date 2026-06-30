package org.example.gen.pbt.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.gen.utils.FlexibleStringList

data class Bound(val name: String, val arb: String)
data class TypedSlot(val name: String, val type: String)
data class Rendered(val bounds: List<Bound>, val body: List<String>)

@Serializable
data class InvariantPlan(
    val decision: Decision,
    val skipReason: String? = null,
    val invariants: List<Invariant>? = null
)

@Serializable
data class Invariant(
    val kind: String,
    val testName: String,
    @Serializable(with = FlexibleStringList::class)
    val args: List<String> = emptyList(),
    val value: String? = null,
    val predicate: String? = null,
    val reference: String? = null,
    val code: String? = null,
    val preconditions: List<Precondition> = emptyList(),
)

@Serializable
data class Precondition(
    val kind: String,
    @Serializable(with = FlexibleStringList::class)
    val args: List<String> = emptyList(),
    val predicate: String? = null,
)

@Serializable
enum class Decision {
    @SerialName("skip") SKIP,
    @SerialName("generate") GENERATE,
}