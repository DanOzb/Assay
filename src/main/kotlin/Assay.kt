package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path

class Assay: CliktCommand() {
    val path by argument(help="Target project path")
        .path(mustExist = true, canBeFile = false)
    override fun run() {
        echo("Target project path: $path")
    }
}
