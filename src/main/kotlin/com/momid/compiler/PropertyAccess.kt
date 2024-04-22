package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val propertyAccess by lazy {
    propertyAccessO
}

fun ExpressionResultsHandlerContext.handlePropertyAccess(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val propertyAccess = this.asMulti()
        var output = ""
        println("is property access: " + this.tokens())
        val firstExpression = propertyAccess[0]
        println("first element: " + firstExpression.tokens)

        val (evaluation, outputType) = continueWithOne(firstExpression, complexExpression) {
            handleComplexExpression(
                currentGeneration
            )
        }.okOrReport {
            if (it is NoExpressionResultsError) {
                println("expected expression. found: " + firstExpression.tokens)
                return Error("expected expression. found: " + firstExpression.tokens, firstExpression.range)
            }
            println(it.error)
            return it.to()
        }

        println("first element: " + this["firstExpression"].tokens + " of type " + outputType.text)

        var currentType = outputType
        output += evaluation

        for (index in 1 until propertyAccess.size) {
            val nextExpression = propertyAccess[index]
            println("other element: " + nextExpression.tokens)

            nextExpression.isOf(variableAccess) {
                println("is variable: " + it.tokens)

                val accessVariableName = it.tokens

                if (currentType is ReferenceType) {
                    if (accessVariableName == "value") {
                        val (evaluation, outputType) = continueStraight(it) { handleReferenceAccess(output, currentType, currentGeneration) }.okOrReport {
                            println(it.error)
                            return it.to()
                        }
                        currentType = outputType
                        output = "(" + evaluation + ")"
                        return@isOf
                    } else {
                        return Error("its not currently possible to access from reference types", it.range)
                    }
                }

                if (currentType is ClassType) {
                    val classType = currentType as ClassType

                    val accessVariableIndex =
                        classType.outputClass.variables.indexOfFirst { it.name == accessVariableName }.apply {
                            if (this == -1) {
                                return Error("unknown property: " + accessVariableName, it.range)
                            }
                        }
                    val cAccessVariable =
                        resolveClass(classType.outputClass, currentGeneration).variables[accessVariableIndex]
                    currentType = classType.outputClass.variables[accessVariableIndex].type
                    if (currentType is TypeParameterType) {
                        currentType = (currentType as TypeParameterType).genericTypeParameter.substitutionType!!
                    }
                    output += "." + cAccessVariable.name
                }
            }

            nextExpression.isOf(functionCall) {
                println("is function call: " + it.tokens())

                val functionReceiver = Eval(output, currentType)

                val (evaluation, outputType) = continueWithOne(it, functionCall) {
                    handleFunctionCall(currentGeneration, functionReceiver)
                }.okOrReport {
                    return it.to()
                }

                currentType = outputType
                output = evaluation
            }

            nextExpression.isOf(arrayAccessItem) {
                println("array access")
                val arrayIndex = it.continuing {
                    return Error("expecting array access index found " + it.tokens, it.range)
                }

                val arrayAccessParsing = ArrayAccessParsing(
                    ExpressionResult(Expression(), firstExpression.range.first..propertyAccess[index - 1].nextTokenIndex, propertyAccess[index - 1].nextTokenIndex).parsing,
                    arrayIndex.parsing,
                    ExpressionResult(Expression(), firstExpression.range.first..propertyAccess[index].nextTokenIndex, propertyAccess[index].nextTokenIndex).parsing
                )

                val (evaluation, outputType) = handleArrayAccess(arrayAccessParsing, currentGeneration).okOrReport {
                    return it.to()
                }

                currentType = outputType
                output = evaluation
            }
        }
        return Ok(Pair(output, currentType))
    }
}

val variableAccess =
    className["propertyName"]

val arrayAccessItem =
    insideOf('[', ']') {
        complexExpression
    }

val propertyAccessO = CustomExpressionValueic() { tokens, startIndex, endIndex, thisExpression ->
    val expressionResults = ArrayList<ExpressionResult>()
    val firstExpression = evaluateExpressionValueic(
        anyOf(atomicExp, simpleExpressionInParentheses)["firstExpression"],
        startIndex,
        tokens,
        endIndex
    ) ?: return@CustomExpressionValueic null
    expressionResults.add(firstExpression)
    var currentIndex = firstExpression.nextTokenIndex
    var isFirstExpression = true
    while (true) {
        val nextExpression = evaluateExpressionValueic(one(!"." + functionCall["functionCall"]), currentIndex, tokens, endIndex)
            ?: evaluateExpressionValueic(one(!"." + variableAccess["propertyAccess"]), currentIndex, tokens, endIndex)
            ?: evaluateExpressionValueic(arrayAccessItem["arrayAccess"], currentIndex, tokens, endIndex)
            ?: if (isFirstExpression) {
                return@CustomExpressionValueic null
            } else {
                break
            }

        isFirstExpression = false

        expressionResults.add(nextExpression)

        currentIndex = nextExpression.nextTokenIndex
        if (currentIndex >= endIndex) {
            break
        }
    }

    return@CustomExpressionValueic MultiExpressionResult(
        ExpressionResult(
            thisExpression,
            startIndex..currentIndex,
            currentIndex
        ), expressionResults
    )
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
    val text = "some.other()".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(propertyAccess))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handlePropertyAccess(currentGeneration)
        }
    }
}
