package com.momid.compiler

class Scope(val scopes: ArrayList<Scope> = ArrayList()): List<Scope> by scopes {

    val variables = ArrayList<VariableInformation>()
}

class VariableInformation(var name: String, var type: Type, var value: Any, var outputName: String, outputType: Type)

class Type(specifier: String)

enum class OutputType {
    Int, Boolean, CharArray
}
