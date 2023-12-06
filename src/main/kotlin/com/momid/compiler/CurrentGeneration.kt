package com.momid.compiler

import com.momid.compiler.output.ClassesInformation
import com.momid.compiler.output.FunctionsInformation
import com.momid.compiler.output.Scope
import com.momid.parser.expression.Error

class CurrentGeneration {

    var rootScope: Scope = Scope()
    var currentScope: Scope = rootScope
    var generatedSource = ""
    var globalDefinitionsGeneratedSource = ""
    var functionDeclarationsGeneratedSource = ""
    val classesInformation = ClassesInformation()
    val functionsInformation = FunctionsInformation(HashMap())
    val errors = ArrayList<Error<*>>()

    private var currentCStructNameNumber = 0
    private var currentCFunctionNameNumber = 0

    fun createScope(): Scope {
        val scope = Scope()
        scope.upperScope = currentScope
        currentScope.scopes.add(scope)
        currentScope = scope
        return scope
    }

    fun createScope(scope: Scope): Scope {
        scope.upperScope = currentScope
        currentScope.scopes.add(scope)
        currentScope = scope
        return scope
    }

    fun goOutOfScope(): Boolean {
        if (currentScope.upperScope != null) {
            currentScope = currentScope.upperScope!!
            return true
        } else {
            return false
        }
    }

    fun addToSource(codeToAdd: String) {
        generatedSource += "\n"
        generatedSource += codeToAdd
    }

    fun constructOutputSource(): String {
        return wholeProgram(generatedSource, globalDefinitionsGeneratedSource, functionDeclarationsGeneratedSource)
    }

    fun createCStructName(): String {
        currentCStructNameNumber += 1
        return "Struct" + currentCStructNameNumber
    }

    fun createCFunctionName(): String {
        currentCFunctionNameNumber += 1
        return "function" + currentCFunctionNameNumber
    }
}
