package com.momid.compiler.output

import com.momid.compiler.Context

var currentVariableNameNumber = 0

class Scope(val scopes: ArrayList<Scope> = ArrayList(), var scopeContext: Context = Context()): List<Scope> by scopes {

    var upperScope: Scope? = null
    val variables = ArrayList<VariableInformation>()
    val classesInformation = ClassesInformation()
    val functionsInformation = FunctionsInformation(hashMapOf())
    var generatedSource = ""
}

fun createVariableName(): String {
    currentVariableNameNumber += 1
    return "variable" + currentVariableNameNumber
}

fun createVariableName(prefix: String): String {
    currentVariableNameNumber += 1
    return prefix + "_variable" + currentVariableNameNumber
}

class VariableInformation(var name: String, var type: Type, var value: Any, var outputName: String, var outputType: OutputType)
