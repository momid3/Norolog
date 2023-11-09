package com.momid.compiler

var currentVariableNameNumber = 0

class Scope(val scopes: ArrayList<Scope> = ArrayList()): List<Scope> by scopes {

    var upperScope: Scope? = null
    val variables = ArrayList<VariableInformation>()
}

fun createVariableName(): String {
    currentVariableNameNumber += 1
    return "variable" + currentVariableNameNumber
}

class VariableInformation(var name: String, var type: Type, var value: Any, var outputName: String, outputType: OutputType)

class Type(specifier: String)

enum class OutputType {
    Int, Boolean, CharArray
}
