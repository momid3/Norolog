package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.compiler.output.VariableInformation
import com.momid.compiler.output.outputInt
import com.momid.compiler.output.outputString
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

val stringLiteral =
    CustomExpression() { tokens, startIndex, endIndex ->
        if (tokens[startIndex] != '"') {
            return@CustomExpression -1
        }
        for (index in startIndex + 1 until endIndex) {
            if (tokens[index] == '"') {
                return@CustomExpression index + 1
            }
        }
        return@CustomExpression -1
    }

val variableNameO by lazy {
    and(condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() }) + not(condition { it == '(' }), not(anyOf(!"in", !"until")))
}

val number =
    condition { it.isDigit() } + some0(dotInTheMiddleOfNumber) + some0(condition { it.isDigit() })

val atomicExp =
    anyOf(variableNameO, number, cf, stringLiteral)

val operator =
    anyOf('+', '-', '*', '/')

val simpleExpression =
    some(inlineContent(anyOf(inlineToOne(spaces + atomicExp["atomicExp"] + spaces), inlineToOne(spaces + operator["operator"] + spaces))))

val expressionInParentheses =
    insideParentheses

val simpleExpressionInParentheses =
    !"(" + expressionInParentheses["insideParentheses"] + ")"

val complexExpression by lazy {
    some(inlineContent(anyOf(simpleExpression, simpleExpressionInParentheses)))
}


private fun handleExpressionResults(expressionFinder: ExpressionFinder, expressionResults: List<ExpressionResult>, tokens: List<Char>) {
    val currentGeneration = CurrentGeneration()
    expressionResults.forEach {
        handleExpressionResult(expressionFinder, it, tokens) {
            handleComplexExpression(currentGeneration)
        }
    }
}

fun ExpressionResultsHandlerContext.resolveVariable(currentGeneration: CurrentGeneration): Result<VariableInformation> {
    val variableName = this.expressionResult.correspondingTokensText(this.tokens)
    var scope = currentGeneration.currentScope
    var foundVariable: VariableInformation? = null
    while (true) {
        scope.variables.forEach {
            if (it.name == variableName) {
                foundVariable = it
                return Ok(it)
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

/***
 * evaluates the value of the expression and returns the value of it that can be inserted to the output
 * and the output type of the value of the expression
 *
 * @return Pair.first: the value that can be inserted to the output
 *
 * Pair.last: the type of the value of the expression
 */
fun ExpressionResultsHandlerContext.handleComplexExpression(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
        this.expressionResult.isOf(complexExpression) {
            var type: OutputType? = null
            var output = ""
            print("complex expression:", it)
            it.forEach {

                it.isOf(simpleExpression) {
                    print("simple:", it)
                    it.forEach {
                        it.isOf(atomicExp) {

                            it.content.isOf(cf) {
                                val evaluatedFunction = continueWithOne(it, cf) { handleCF(currentGeneration) }
                                if (evaluatedFunction is Ok) {
                                    type = evaluatedFunction.ok.second
                                    output += evaluatedFunction.ok.first
                                }
                                if (evaluatedFunction is Error<*>) {
                                    currentGeneration.errors.add(evaluatedFunction)
                                    println(evaluatedFunction.error)
                                }
                            }

                            it.content.isOf(variableNameO) {
                                print("variable:", it)
                                val outputVariable = continueStraight(it) {
                                    resolveVariable (currentGeneration)
                                }
                                if (outputVariable is Ok<VariableInformation>) {
                                    type = outputVariable.ok.outputType
                                    output += outputVariable.ok.outputName
                                }
                                if (outputVariable is Error<*>) {
                                    currentGeneration.errors.add(outputVariable)
                                    println(outputVariable.error)
                                }
                            }

                            it.content.isOf(number) {
                                println("is number " + it.correspondingTokensText(tokens))
                                type = OutputType(outputInt)
                                output += it.correspondingTokensText(tokens)
                            }

                            it.content.isOf(stringLiteral) {
                                println("is string literal: " + it.tokens())
                                val text = it.tokens()
                                type = OutputType(outputString)
                                output += text
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
                    if (evaluation is Ok<Pair<String, OutputType>>) {
                        type = evaluation.ok.second
                        output += "(" + evaluation.ok.first + ")"
                    }
                    if (evaluation is Error<*>) {
                        currentGeneration.errors.add(evaluation)
                        println(evaluation.error)
                    }
                }
            }
            if (type == null) {
                return Error("could not determine type of this expression: " + it.tokens(), it.range)
            } else {
                return Ok(Pair(output, type!!))
            }
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
