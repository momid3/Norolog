package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.isOf
import com.momid.parser.not

val dotInTheMiddleOfNumber = CustomExpression() { tokens, startIndex, endIndex ->
    if (tokens[startIndex] == '.' && startIndex > 0 && tokens[startIndex - 1].isDigit() && startIndex < tokens.lastIndex && tokens[startIndex + 1].isDigit()) {
        return@CustomExpression startIndex + 1
    } else {
        return@CustomExpression -1
    }
}

val variableName =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() }) + not(condition { it == '(' })

val number =
    condition { it.isDigit() } + some0(dotInTheMiddleOfNumber) + some0(condition { it.isDigit() })

val atomicExp =
    anyOf(variableName, number, function)

val operator =
    anyOf('+', '-', '*', '/')

val simpleExpression =
    some(inlineContent(anyOf(inlineToOne(spaces + atomicExp["atomicExp"] + spaces), inlineToOne(spaces + operator["operator"] + spaces))))

val expressionInParentheses =
    insideParentheses

val simpleExpressionInParentheses =
    !"(" + expressionInParentheses["insideParentheses"] + ")"

val complexExpression =
    some(inlineContent(anyOf(simpleExpression, simpleExpressionInParentheses)))


private fun handleExpressionResults(expressionFinder: ExpressionFinder, expressionResults: List<ExpressionResult>, tokens: List<Char>) {
    val currentGeneration = CurrentGeneration()
    expressionResults.forEach {
        handleExpressionResult(expressionFinder, it, tokens) {
            handleComplexExpression(currentGeneration)
        }
    }
}

fun ExpressionResultsHandlerContext<String>.resolveVariable(currentGeneration: CurrentGeneration): Result<String> {
    val variableName = this.expressionResult.correspondingTokensText(this.tokens)
    var scope = currentGeneration.currentScope
    var foundVariable: VariableInformation? = null
    while (true) {
        scope.variables.forEach {
            if (it.name == variableName) {
                foundVariable = it
                return Ok(it.outputName)
            }
        }
        if (scope.upperScope != null) {
            scope = scope.upperScope!!
        } else {
            break
        }
    }
    return Error("could not resolve variable: " + this.expressionResult.tokens(), this.expressionResult.range)
}

fun ExpressionResultsHandlerContext<String>.handleComplexExpression(currentGeneration: CurrentGeneration): Result<String> {
        this.expressionResult.isOf(complexExpression) {
            var output = ""
            print("complex expression:", it)
            it.forEach {

                it.isOf(simpleExpression) {
                    print("simple:", it)
                    it.forEach {
                        it.isOf("atomicExp") {

                            it.content.isOf(function) {
                                val evaluatedFunction = continueWithOne(it, function) { handleFunction(currentGeneration) }
                                if (evaluatedFunction is Ok<String>) {
                                    output += evaluatedFunction.ok
                                }
                                if (evaluatedFunction is Error<*>) {
                                    currentGeneration.errors.add(evaluatedFunction)
                                    println(evaluatedFunction.error)
                                }
                            }

                            it.content.isOf(variableName) {
                                print("variable:", it)
                                val outputVariable = continueStraight(it) {
                                    resolveVariable (currentGeneration)
                                }
                                if (outputVariable is Ok<String>) {
                                    output += outputVariable.ok
                                }
                                if (outputVariable is Error<*>) {
                                    currentGeneration.errors.add(outputVariable)
                                    println(outputVariable.error)
                                }
                            }

                            it.content.isOf(number) {
                                println("is number " + it.correspondingTokensText(tokens))
                                output += it.correspondingTokensText(tokens)
                            }
                        }

                        it.isOf("operator") {
                            println("is operator " + it.correspondingTokensText(tokens))
                            output += it.correspondingTokensText(tokens)
                        }
                    }
                }

                it.isOf(simpleExpressionInParentheses) {
                    print("simple in parentheses:", it)
                    val evaluation = continueWithOne(it["insideParentheses"], complexExpression) { handleComplexExpression(currentGeneration) }
                    if (evaluation is Ok<*>) {
                        output += evaluation.ok
                    }
                    if (evaluation is Error<*>) {
                        currentGeneration.errors.add(evaluation)
                        println(evaluation.error)
                    }
                }
            }
            return Ok(output)
        }
    return Error("", IntRange.EMPTY)
}

fun main() {

    val text = "someVar + 3 + some + someFunction(3, 7, 3) + 7 + (3 + 37 + (373 + 373))".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(complexExpression))
    val expressionResults = finder.start(text)
    handleExpressionResults(finder, expressionResults, text)
}
