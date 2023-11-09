package com.momid.compiler

import com.momid.parser.expression.*

val text = "print(3 + 7 + 37)".toList()

val statements = listOf(assignment, functionCall)

val statementsExp =
    some(inlineToOne(spaces + anyOf(*statements.toTypedArray())["statement"] + spaces))

fun ExpressionResultsHandlerContext<String>.handleStatements(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(statementsExp) {
        it.forEach {
            println("is statement: " + it.tokens())
            it.content.isOf(functionCall) {
                println("is function call")
                continueStraight(it) {
                    handleFunctionCall(currentGeneration)
                }
            }

            it.content.isOf(assignment) {
                continueStraight(it) {
                    handleAssignment(currentGeneration)
                }
            }
        }
    }
    return Ok("")
}

fun ExpressionResultsHandlerContext<String>.handleCodeBlock(currentGeneration: CurrentGeneration): Result<String> {
    currentGeneration.createScope()
    continueWith(this.expressionResult, *statements.toTypedArray()) {
        handleStatements(currentGeneration)
    }
    return Ok("")
}

fun main() {
    val text = "print(3 + 7 + 37);".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(statementsExp))

    val currentGeneration = CurrentGeneration()
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleStatements(currentGeneration)
        }
    }

    println(currentGeneration.generatedSource)
}