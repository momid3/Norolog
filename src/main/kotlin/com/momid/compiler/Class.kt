package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val classVariableExp =
    variableNameO["variableName"] + spaces + ":" + spaces + variableNameO["variableType"]

val classVariable =
    ignoreParentheses(condition { it != ',' && it != '}' })

val classVariables =
    spaces + inline(classVariable["classVariable"] + inline(some0(spaces + !"," + spaces + classVariable["classVariable"])) + spaces) + spaces

val klass =
    !"class" + space + variableNameO["className"] + spaces + insideOf("classInside", '{', '}')

fun ExpressionResultsHandlerContext.handleClass(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(klass) {
        val classVariablesOutput = ArrayList<ClassVariable>()
        println("class definition: " + it.tokens())
        println("class content: " + it["classInside"].content.tokens())
        val className = it["className"].tokens()
        val classVariablesS = continueWithOne(it["classInside"].content, classVariables) { handleClassVariables(currentGeneration) }

        classVariablesS.handle({
            return Error(it.error, it.range)
        }, {
            it.forEach {
                println("class variable: " + it.name + ": " + it.type)
                val classVariable = ClassVariable(it.name, OutputType(resolveType(it.type, currentGeneration)))
                classVariablesOutput.add(classVariable)
            }

            val outputClass = Class(className, classVariablesOutput)
            val outputStruct = CStruct(currentGeneration.createCStructName(), classVariablesOutput.map {
                CStructVariable(it.name, resolveType(it.type.outputClass, currentGeneration))
            })

            currentGeneration.classesInformation.classes[outputClass] = outputStruct

            currentGeneration.globalDefinitionsGeneratedSource += cStruct(outputStruct.name, outputStruct.variables.map { Pair(it.name, it.type.name) }) + "\n"
        })
    }
    println("is not class")
    return Error("is not class", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleClassVariables(currentGeneration: CurrentGeneration): Result<List<ClassVariableS>> {
    this.expressionResult.isOf(classVariables) {
        var hasErrors = false
        val classVariables = ArrayList<ClassVariableS>()
        it.forEach {
            it.forEach {
                val classVariable = continueWithOne(it, classVariableExp) { handleClassVariable() }

                classVariable.handle({
                    currentGeneration.errors.add(it)
                }, {
                    classVariables.add(it)
                })
            }
        }
        return Ok(classVariables)
    }
    println("is not class variables")
    return Error("is not class variables", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleClassVariable(): Result<ClassVariableS> {
    this.expressionResult.isOf(classVariableExp) {
        val name = it["variableName"].tokens()
        val type = it["variableType"].tokens()
        return Ok(ClassVariableS(name, type))
    }
    return Error("is not class variable", this.expressionResult.range)
}

fun resolveClass(outputType: Class, currentGeneration: CurrentGeneration): CStruct {
    return currentGeneration.classesInformation.classes[outputType] ?: throw (Throwable("could not resolve corresponding CStruct to this class"))
}

fun resolveType(outputType: Class, currentGeneration: CurrentGeneration): Type {
    if (outputType == outputInt) {
        return Type.Int
    } else if (outputType == outputString) {
        return Type.CharArray
    } else {
        return Type(currentGeneration.classesInformation.classes[outputType]?.name ?: throw (Throwable("could not resolve corresponding CStruct to this class")))
    }
}

fun resolveType(outputTypeName: String, currentGeneration: CurrentGeneration): Class {
    return currentGeneration.classesInformation.classes.entries.find {
        it.key.name == outputTypeName
    }?.key ?: throw (Throwable("class with this name not found: " + outputTypeName))
}

inline fun <T> Result<T>.handle(error: (Error<T>) -> Unit = {  }, ok: (T) -> Unit) {
    if (this is Ok) {
        ok(this.ok)
    }

    if (this is Error) {
        error(this)
    }
}

class ClassVariableS(val name: String, val type: String)

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "class SomeClass { someVariable: SomeType, anotherVariable: AnotherType, someOtherVariable: SomeOtherType }".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(klass))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleClass(currentGeneration)
        }
    }
}
