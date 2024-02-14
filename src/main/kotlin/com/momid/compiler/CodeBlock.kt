package com.momid.compiler

import com.momid.compiler.output.Scope
import com.momid.parser.expression.*

val text = "print(3 + 7 + 37)".toList()

val statements = listOf(
    variableDeclaration,
    functionCallStatement,
    forStatement,
    propertyAccessStatement,
    info,
    gClass,
    functionDeclaration,
    returnStatement,
    assignment,
    classFunction
)

val statementsExp =
    some(spaces + anyOf(*statements.toTypedArray())["statement"] + spaces)

fun ExpressionResultsHandlerContext.handleStatements(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(statementsExp) {
        it.forEach {
            println("is statement: " + it.tokens())
            with(it["statement"]) {
                content.isOf(functionCallStatement) {
                    println("is function call")
                    continueStraight(it) {
                        handleFunctionCallStatement(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(variableDeclaration) {
                    continueStraight(it) {
                        handleVariableDeclaration(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(forStatement) {
                    println("is for loop")
                    continueStraight(it) {
                        handleForLoop(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(gClass) {
                    continueStraight(it) {
                        handleClassDeclaration(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(functionDeclaration) {
                    continueStraight(it) {
                        handleFunctionDeclaration(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(returnStatement) {
                    continueStraight(it) {
                        handleReturnStatement(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(assignment) {
                    continueStraight(it) {
                        handleAssignment(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(classFunction) {
                    continueStraight(it) {
                        handleClassFunction(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(info) {
                    continueStraight(it) {
                        handleInfo(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(propertyAccessStatement) {
                    continueStraight(it) {
                        handlePropertyAccessStatement(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
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
