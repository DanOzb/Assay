package org.example.core

data class ParsedParam(val name: String, val type: String)
data class AnnotationModel(
    val name: String,
    val arguments: List<String> = emptyList()
)

data class ParsedFunction(
    val name: String,
    val fullName: String,
    val receiver: String?,
    val params: List<ParsedParam>,
    val returnType: String,
    val visibility: String,
    val docs: String?,
    val annotations: List<AnnotationModel>,
    val body: String?,
    val isSuspend: Boolean,
    val callability: Callability = Callability.TOP_LEVEL,
    val container: ContainerModel? = null,
    )

enum class Callability { TOP_LEVEL, SINGLETON, REQUIRES_INSTANCE, LOCAL, }
enum class ContainerKind { CLASS, INTERFACE, OBJECT, COMPANION_OBJECT, ENUM }
data class ContainerModel(
    val name: String,
    val fqName: String,
    val kind: ContainerKind,
    val instanceStructure: InstanceStructure,
    val isInner: Boolean = false,
    val hasMutableState: Boolean = false,
)

sealed interface InstanceStructure {
    data object Singleton : InstanceStructure
    data class Construct(val params: List<ParsedParam>) : InstanceStructure
    data class EnumEntries(val entries: List<String>) : InstanceStructure
    data class NotConstructible(val reason: String) : InstanceStructure
}
