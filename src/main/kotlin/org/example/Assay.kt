package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import org.example.core.Parser
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.standalone.disposeGlobalStandaloneApplicationServices

@OptIn(KaExperimentalApi::class)
class Assay: CliktCommand() {
    val path by argument(help="Target project path")
        .path(mustExist = true, canBeFile = false)
    override fun run() {
        val parser = Parser()
        parser.inspectDir(path)

        //To stop global application services
        disposeGlobalStandaloneApplicationServices()
    }
}