package com.momid.compiler

import com.momid.parser.expression.Error

class CurrentGeneration {

    var rootScope: Scope = Scope()
    var currentScope: Scope = rootScope
    var generatedSource = ""
    val errors = ArrayList<Error<*>>()

    fun createScope(): Scope {
        val scope = Scope()
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
        return wholeProgram(generatedSource)
    }
}
