package com.momid.compiler.output

var currentVariableNameNumber = 0

class Scope(val scopes: ArrayList<Scope> = ArrayList()): List<Scope> by scopes {

    var upperScope: Scope? = null
    val variables = ArrayList<VariableInformation>()
    var generatedSource = ""
}

fun createVariableName(): String {
    currentVariableNameNumber += 1
    return "variable" + currentVariableNameNumber
}

class VariableInformation(var name: String, var type: Type, var value: Any, var outputName: String, var outputType: OutputType)

class OutputType(val outputClass: Class, val specifier: String = "")

class Type(val name: String, val specifier: String = "") {
    companion object {
        val Int = Type("int")
        val Boolean = Type("bool")
        val CharArray = Type("CharArray")
    }
}
