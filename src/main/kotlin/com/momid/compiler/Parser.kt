package com.momid.compiler

import com.momid.compiler.output.*
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

val stringLiteral by lazy {
    CustomExpression() { tokens, startIndex, endIndex ->
        if (startIndex >= endIndex) {
            return@CustomExpression -1
        }
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
}

val trueValue by lazy {
    !"true"
}

val falseValue by lazy {
    !"false"
}

val builtinValues by lazy {
    anyOf(trueValue, falseValue)
}

val variableNameO by lazy {
    and(condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() }) + not(condition { it == '(' }), not(anyOf(!"in", !"until")))
}

val number by lazy {
    condition { it.isDigit() } + some0(dotInTheMiddleOfNumber) + some0(condition { it.isDigit() })
}

val atomicExp =
    anyOf(builtinValues, variableNameO, number, cf, infoAccess, stringLiteral, arrayInitialization)["atomic"] + not(!".")

val operator =
    anyOf('+', '-', '*', '/')

val simpleExpression: RecurringSomeExpression by lazy {
    some(inlineContent(anyOf(
        inlineToOne(spaces + arrayAccess["arrayAccess"] + spaces),
        inlineToOne(spaces + atomicExp["atomicExp"] + spaces),
        inlineToOne(spaces + operator["operator"] + spaces),
        inlineToOne(spaces + propertyAccess["propertyAccess"] + spaces)
    )))
}

val expressionInParentheses =
    insideParentheses

val simpleExpressionInParentheses by lazy {
    !"(" + expressionInParentheses["insideParentheses"] + ")"
}

val complexExpression by lazy {
    spaces + some(inlineContent(anyOf(simpleExpression, simpleExpressionInParentheses)))["complexExpression"] + spaces
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
            if (it.outputName == variableName) {
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

fun resolveVariable(variableName: String, currentGeneration: CurrentGeneration): VariableInformation? {
    var scope = currentGeneration.currentScope
    var foundVariable: VariableInformation? = null
    while (true) {
        scope.variables.forEach {
            if (it.name == variableName) {
                foundVariable = it
                return it
            }
        }
        if (scope.upperScope != null) {
            scope = scope.upperScope!!
        } else {
            break
        }
    }
    return null
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
            it["complexExpression"].forEach {

                it.isOf(simpleExpression) {
                    print("simple:", it)
                    it.forEach {
                        it.isOf(atomicExp) {

                            it["atomic"].content.isOf(cf) {
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

                            it["atomic"].content.isOf(variableNameO) {
                                print("variable:", it)
                                val outputVariable = continueStraight(it) {
                                    resolveVariable (currentGeneration)
                                }
                                if (outputVariable is Ok<VariableInformation>) {
                                    type = outputVariable.ok.outputType
                                    output += outputVariable.ok.name
                                }
                                if (outputVariable is Error<*>) {
                                    currentGeneration.errors.add(outputVariable)
                                    println(outputVariable.error)
                                }
                            }

                            it["atomic"].content.isOf(number) {
                                println("is number " + it.correspondingTokensText(tokens))
                                type = ClassType(outputInt)
                                output += it.correspondingTokensText(tokens)
                            }

                            it["atomic"].content.isOf(stringLiteral) {
                                println("is string literal: " + it.tokens())
                                val text = it.tokens()
                                type = ClassType(outputString)
                                output += text
                            }

                            it["atomic"].content.isOf(arrayInitialization) {
                                val (evaluation, outputType) = continueStraight(it) { handleArrayInitialization(currentGeneration) }.okOrReport {
                                    return it.to()
                                }
                                type = outputType
                                output += evaluation
                            }

                            it["atomic"].content.isOf(builtinValues) {
                                it.content.isOf(trueValue) {
                                    type = outputBooleanType
                                    output += "true"
                                }

                                it.content.isOf(falseValue) {
                                    type = outputBooleanType
                                    output += "false"
                                }
                            }

                            it["atomic"].content.isOf(infoAccess) {
                                val (evaluation, outputType) = continueStraight(it) { handleInfoAccess(currentGeneration) }.okOrReport {
                                    return it.to()
                                }
                                type = outputType
                                output += evaluation
                            }
                        }

                        it.isOf(propertyAccess) {
                            val propertyAccessEvaluation = continueStraight(it) { handlePropertyAccess(currentGeneration) }.okOrReport {
                                println(it.error)
                                return it.to()
                            }
                            type = propertyAccessEvaluation.second
                            output += propertyAccessEvaluation.first
                        }

                        it.isOf(arrayAccess) {
                            val (evaluation, outputType) = continueStraight(it) { handleArrayAccess(currentGeneration) }.okOrReport {
                                return it.to()
                            }
                            type = outputType
                            output += evaluation
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
                        return evaluation.to()
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

//    val text = "someVar + 3 + some + someFunction(3, 7, 3) + 7 + (3 + 37 + (373 + 373))".toList()
    val text = "(someVar + 3 + some.theVariable + someFunction(3, 7, 3) + 7 + (3 + 37 + (373 + 373))))))))".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(complexExpression))
    val expressionResults = finder.start(text)
    handleExpressionResults(finder, expressionResults, text)
}
