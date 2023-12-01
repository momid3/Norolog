package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*
import com.momid.parser.not

val propertyAccessFirstElement by lazy {
    anyOf(anyOf(variableNameO, number, cf, stringLiteral)["atomic"], simpleExpressionInParentheses["expressionInParentheses"])
}

val propertyAccessVariable by lazy {
    anyOf(variableNameO, function)
}

val propertyAccessElement by lazy {
    ignoreParentheses(condition { it != '.'  && it != ')' && it != ' '})
}

val propertyAccess by lazy {
    spaces + propertyAccessFirstElement["firstExpression"] + inline(some(spaces + !"." + spaces + propertyAccessElement["element"])["otherElements"]) + spaces
}

fun ExpressionResultsHandlerContext.handlePropertyAccess(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    this.expressionResult.isOf(propertyAccess) {
        var output = ""
        println("is property access: " + it.tokens())
        val firstElement = it["firstExpression"]
        println("first element: " + firstElement.tokens())
        val firstElementEvaluation = continueWithOne(firstElement, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            if (it is NoExpressionResultsError) {
                println("expected expression. found: " + firstElement.tokens())
                return Error("expected expression. found: " + firstElement.tokens(), firstElement.range)
            }
            println(it.error)
            return it.to()
        }
        val firstElementValue = firstElementEvaluation.first
        val firstElementType = firstElementEvaluation.second
        println("first element: " + it["firstExpression"].tokens())
        var currentType = firstElementType
        output += firstElementValue

        it["otherElements"].forEach {
            println("other element: " + it.tokens())
            require(it, propertyAccessVariable, {
                println("expecting variable or function call. found: " + it.tokens())
                return Error("expecting variable or function call. found: " + it.tokens(), it.range)
            }) {
                it.content.isOf(variableNameO) {
                    println("is variable: " + it.tokens())
                    val accessVariableName = it.tokens()
                    val accessVariableIndex = currentType.outputClass.variables.indexOfFirst { it.name == accessVariableName }.apply {
                        if (this == -1) {
                            return Error("unknown property: " + accessVariableName, it.range)
                        }
                    }
                    val cAccessVariable = (currentGeneration.classesInformation.classes[currentType.outputClass] ?: throw(Throwable("class not found"))).variables[accessVariableIndex]
                    currentType = currentType.outputClass.variables[accessVariableIndex].type
                    output += "." + cAccessVariable.name

                }

                it.content.isOf(function) {
                    println("is function call: " + it.tokens())
                }
            }
        }
        return Ok(Pair(output, currentType))
    }
    return Error("is not property", this.expressionResult.range)
}

inline fun <T> Result<T>.okOrReport(report: (error: Error<T>) -> Unit): T {
    if (this is Ok) {
        return this.ok
    } else {
        report(this as Error<T>)
    }
    throw (Throwable("you should have returned in the report function"))
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "someVariable.anotherVariable.someFunction()".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(propertyAccess))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handlePropertyAccess(currentGeneration)
        }
    }
}
