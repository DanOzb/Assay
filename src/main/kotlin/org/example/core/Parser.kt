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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.Variance
import java.nio.file.Path

class Parser {
    fun inspectDir(path: Path): List<ParsedFunction>{
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
                analyze(file){
                    file.declarations.filterIsInstance<KtNamedFunction>()
                        .map { parseFunction(it) }
                }
            }
        } finally {
            Disposer.dispose(projectDisposable)
        }
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
            isSuspend = fn.hasModifier(KtTokens.SUSPEND_KEYWORD)
        )
    }

    fun KtNamedFunction.parseAnnotations(): List<AnnotationModel> =
        annotationEntries.map { entry ->
            AnnotationModel(
                name = entry.shortName?.asString() ?: entry.text,
                arguments = entry.valueArguments.map { arg ->
                    val argName = arg.getArgumentName()?.asName?.asString()
                    val value = arg.getArgumentExpression()?.text.orEmpty()
                    if(argName != null) "$argName=$value" else value
                }
            )
        }
}
