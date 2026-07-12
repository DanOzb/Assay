package org.example.core

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.Variance
import java.nio.file.Path

class Parser {
    fun inspectDir(path: Path): List<ParsedFunction> {
        val projectDisposable = Disposer.newDisposable("MyStandaloneAnalysisSession")
        try {
            val session = buildStandaloneAnalysisAPISession(projectDisposable = projectDisposable) {
                buildKtModuleProvider {
                    val jvm = JvmPlatforms.defaultJvmPlatform
                    platform = jvm

                    addModule(
                        buildKtSourceModule {
                            moduleName = "sample-target"
                            platform = jvm
                            addSourceRoot(path)
                        }
                    )
                }
            }

            val ktFiles: List<KtFile> = session.modulesWithFiles.values.flatten().filterIsInstance<KtFile>()

            return ktFiles.flatMap { file ->
                analyze(file) {
                    collectNamedFunctions(file).map { parseFunction(it) }
                }
            }
        } finally {
            Disposer.dispose(projectDisposable)
        }
    }

    private fun collectNamedFunctions(file: KtFile): List<KtNamedFunction> {
        val found = mutableListOf<KtNamedFunction>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                found += function
                super.visitNamedFunction(function)
            }
        })
        return found
    }

    @OptIn(KaExperimentalApi::class)
    fun KaSession.parseFunction(fn: KtNamedFunction): ParsedFunction {
        val symbol = fn.symbol

        return ParsedFunction(
            name = symbol.name?.asString() ?: "anonymous",
            fullName = symbol.callableId?.asSingleFqName()?.asString() ?: symbol.name?.asString().orEmpty(),
            receiver = symbol.receiverParameter?.returnType?.render(
                KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.INVARIANT
            ),
            params = symbol.valueParameters.map { p ->
                ParsedParam(
                    name = p.name.asString(),
                    type = p.returnType.render(
                        KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.INVARIANT
                    )
                )
            },
            returnType = symbol.returnType.render(
                KaTypeRendererForSource.WITH_SHORT_NAMES, Variance.INVARIANT
            ),
            visibility = symbol.visibility.name,
            docs = fn.docComment?.text,
            annotations = fn.parseAnnotations(),
            body = fn.bodyExpression?.text,
            isSuspend = fn.hasModifier(KtTokens.SUSPEND_KEYWORD),
            origin = originOf(fn),
            packageName = fn.containingKtFile.packageFqName.asString(),
        )
    }

    fun KtNamedFunction.parseAnnotations(): List<AnnotationModel> =
        annotationEntries.map { entry ->
            AnnotationModel(
                name = entry.shortName?.asString() ?: entry.text,
                arguments = entry.valueArguments.map { arg ->
                    val argName = arg.getArgumentName()?.asName?.asString()
                    val value = arg.getArgumentExpression()?.text.orEmpty()
                    if (argName != null) "$argName=$value" else value
                }
            )
        }

    private fun KaSession.originOf(fn: KtNamedFunction): Origin {
        if (fn.isLocal) return Origin.Local
        val container = fn.containingClassOrObject ?: return Origin.TopLevel
        if (container.isLocal || container.name == null) return Origin.Local
        return Origin.Member(parseContainer(container))
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.parseContainer(cls: KtClassOrObject): ContainerModel {
        val kind = containerKindOf(cls)
        return ContainerModel(
            name = cls.name ?: "<anonymous>",
            fqName = cls.fqName?.asString() ?: cls.name.orEmpty(),
            kind = kind,
            instanceStructure = getInstanceStructure(cls, kind),
            isInner = (cls as? KtClass)?.isInner() == true,
            hasMutableState = hasMutableState(cls),
        )
    }

    private fun containerKindOf(cls: KtClassOrObject): ContainerKind = when (cls) {
        is KtObjectDeclaration if cls.isCompanion() -> ContainerKind.COMPANION_OBJECT
        is KtObjectDeclaration -> ContainerKind.OBJECT
        is KtClass if cls.isInterface() -> ContainerKind.INTERFACE
        is KtClass if cls.isEnum() -> ContainerKind.ENUM
        else -> ContainerKind.CLASS
    }

    private fun KaSession.getInstanceStructure(
        cls: KtClassOrObject,
        kind: ContainerKind,
    ): InstanceStructure = when (kind) {
        ContainerKind.OBJECT,
        ContainerKind.COMPANION_OBJECT -> InstanceStructure.Singleton

        ContainerKind.INTERFACE -> InstanceStructure.NotConstructible("interface")

        ContainerKind.ENUM -> InstanceStructure.EnumEntries(cls.enumEntryNames())

        ContainerKind.CLASS -> classInstanceStructure(cls)
    }

    private fun KtClassOrObject.enumEntryNames(): List<String> =
        declarations.filterIsInstance<KtEnumEntry>().mapNotNull { it.name }

    private fun KaSession.classInstanceStructure(cls: KtClassOrObject): InstanceStructure {
        nonConstructibleReason(cls)?.let { return InstanceStructure.NotConstructible(it) }
        return InstanceStructure.Construct(constructorParams(cls))
    }

    private fun nonConstructibleReason(cls: KtClassOrObject): String? = when {
        cls is KtClass && cls.isAnnotation() -> "annotation class"
        cls.hasModifier(KtTokens.ABSTRACT_KEYWORD) -> "abstract class"
        cls.hasModifier(KtTokens.SEALED_KEYWORD) -> "sealed class"
        cls.primaryConstructor?.isNonPublic() == true -> "non-public primary constructor"
        else -> null
    }

    private fun KtPrimaryConstructor.isNonPublic(): Boolean =
        hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.PROTECTED_KEYWORD)

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.constructorParams(cls: KtClassOrObject): List<ParsedParam> =
        cls.primaryConstructor
            ?.valueParameters
            ?.map { toParsedParam(it) }
            .orEmpty()

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.toParsedParam(param: KtParameter): ParsedParam =
        ParsedParam(
            name = param.name ?: "_",
            type = param.symbol.returnType.render(
                KaTypeRendererForSource.WITH_SHORT_NAMES,
                Variance.INVARIANT,
            ),
        )

    private fun hasMutableState(cls: KtClassOrObject): Boolean =
        cls.primaryConstructor?.valueParameters?.any { it.isMutable } == true ||
                cls.declarations.filterIsInstance<KtProperty>().any { it.isVar }
}