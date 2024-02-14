package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.Error

class CurrentGeneration {

    var rootScope: Scope = Scope()
    var currentScope: Scope = rootScope
    var generatedSource = ""
    var globalDefinitionsGeneratedSource = ""
    var functionDeclarationsGeneratedSource = ""
    val classesInformation = ClassesInformation()
    val functionsInformation = FunctionsInformation(HashMap())
    val infosInformation = InfosInformation(hashMapOf())
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

    fun createCStructName(prefix: String): String {
        currentCStructNameNumber += 1
        return prefix + "Struct" + currentCStructNameNumber
    }

    fun createCFunctionName(): String {
        currentCFunctionNameNumber += 1
        return "function" + currentCFunctionNameNumber
    }

    private fun addVariable(name: String, cName: String, outputType: OutputType) {
        currentScope.variables += VariableInformation(cName, resolveType(outputType, this), 0, name, outputType)
    }

    private fun addVariable(name: String, cName: String, outputType: OutputType, scope: Scope) {
        scope.variables += VariableInformation(cName, resolveType(outputType, this), 0, name, outputType)
    }

    /***
     * @param name name of the variable
     * @return c variable name of the created variable
     */
    fun createVariable(name: String, outputType: OutputType): String {
        val cVariableName = createVariableName()
        addVariable(name, cVariableName, outputType)
        return cVariableName
    }

    /***
     * @return a pair of created variable names. first
     * is the norolog variable name and last is the c variable name
     */
    fun createVariable(outputType: OutputType): Pair<String, String> {
        val variableName = createVariableName()
        val cVariableName = createVariableName()
        addVariable(variableName, cVariableName, outputType)
        return Pair(variableName, cVariableName)
    }

    /***
     * @param name name of the variable
     * @return c variable name of the created variable
     */
    fun createVariable(name: String, outputType: OutputType, scope: Scope): String {
        val cVariableName = createVariableName()
        addVariable(name, cVariableName, outputType, scope)
        return cVariableName
    }

    /***
     * @return a pair of created variable names. first
     * is the norolog variable name and last is the c variable name
     */
    fun createVariable(outputType: OutputType, scope: Scope): Pair<String, String> {
        val variableName = createVariableName()
        val cVariableName = createVariableName()
        addVariable(variableName, cVariableName, outputType, scope)
        return Pair(variableName, cVariableName)
    }
}
