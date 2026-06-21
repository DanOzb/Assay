package org.example.core

data class ParsedParam(val name: String, val type: String)

data class ParsedFunction(
    val name: String,
    val fullName: String,
    val receiver: String?,
    val params: List<ParsedParam>,
    val returnType: String,
    val visibility: String,
)