package com.momid.compiler

import com.momid.compiler.output.Scope
import com.momid.parser.expression.*

val text = "print(3 + 7 + 37)".toList()

val statements = listOf(assignment, functionCall, forStatement, klass, functionDeclaration)

val statementsExp =
    some(spaces + anyOf(*statements.toTypedArray())["statement"] + spaces)

fun ExpressionResultsHandlerContext.handleStatements(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(statementsExp) {
        it.forEach {
            println("is statement: " + it.tokens())
            with(it["statement"]) {
                content.isOf(functionCall) {
                    println("is function call")
                    continueStraight(it) {
                        handleFunctionCall(currentGeneration)
                    }
                }

                content.isOf(assignment) {
                    continueStraight(it) {
                        handleAssignment(currentGeneration)
                    }
                }

                content.isOf(forStatement) {
                    println("is for loop")
                    continueStraight(it) {
                        handleForLoop(currentGeneration)
                    }
                }

                content.isOf(klass) {
                    continueStraight(it) {
                        handleClass(currentGeneration)
                    }
                }

                content.isOf(functionDeclaration) {
                    continueStraight(it) {
                        handleFunctionDeclaration(currentGeneration)
                    }
                }
            }
        }
    }
    return Ok("")
}

fun ExpressionResultsHandlerContext.handleCodeBlock(currentGeneration: CurrentGeneration): Result<String> {
    val scope = currentGeneration.createScope()
    continueWith(this.expressionResult, statementsExp) {
        handleStatements(currentGeneration)
    }
    currentGeneration.goOutOfScope()
    return Ok(scope.generatedSource)
}

fun ExpressionResultsHandlerContext.handleCodeBlock(currentGeneration: CurrentGeneration, scope: Scope): Result<String> {
    currentGeneration.createScope(scope)
    continueWith(this.expressionResult, statementsExp) {
        handleStatements(currentGeneration)
    }
    currentGeneration.goOutOfScope()
    return Ok(scope.generatedSource)
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
