package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.compiler.output.Scope
import com.momid.compiler.output.norType
import com.momid.parser.expression.*

val text = "print(3 + 7 + 37)".toList()

val statements = listOf(
    variableDeclaration,
    functionCallStatement,
    forStatement,
    propertyAccessStatement,
    info,
    gClass,
    returnStatement,
    assignment,
    classFunction,
    cExpression,
    cCodeBlock,
    cClassMapping
)

val statementsExp =
    some(spaces + anyOf(*statements.toTypedArray())["statement"] + spaces)

fun ExpressionResultsHandlerContext.handleStatements(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    var outputType: OutputType = norType
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
                        outputType = it
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

                content.isOf(cExpression) {
                    continueStraight(it) {
                        handleCExpression(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(cCodeBlock) {
                    continueStraight(it) {
                        handleCCodeBlock(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }

                content.isOf(cClassMapping) {
                    continueStraight(it) {
                        handleCClassMapping(currentGeneration)
                    }.handle({
                        currentGeneration.errors.add(it)
                    }, {

                    })
                }
            }
        }
    }
    return Ok(Pair("", outputType))
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

/***
 * @return the c output code and the outputType of the code block.
 */
fun ExpressionResultsHandlerContext.handleCodeBlockWithOutputType(currentGeneration: CurrentGeneration, scope: Scope): Result<Pair<String, OutputType>> {
    currentGeneration.createScope(scope)
    val (evaluation, outputType) = continueWithOne(this.expressionResult, statementsExp) {
        handleStatements(currentGeneration)
    }.okOrReport {
        return it.to()
    }
    currentGeneration.goOutOfScope()
    return Ok(Pair(scope.generatedSource, outputType))
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
