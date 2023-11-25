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

class OutputType(val outputClass: Class, val specifier: String = "") {
    override fun equals(other: Any?): Boolean {
        return other is OutputType && other.outputClass.name == this.outputClass.name
    }

    override fun hashCode(): Int {
        var result = outputClass.hashCode()
        result = 31 * result + specifier.hashCode()
        return result
    }
}

val nothingOutputType = OutputType(outputNothing)

class Type(val name: String, val specifier: String = "") {
    companion object {
        val Int = Type("int")
        val Boolean = Type("bool")
        val CharArray = Type("CharArray")
    }
}
