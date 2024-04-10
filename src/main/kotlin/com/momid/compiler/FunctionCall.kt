package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.standard_library.*
import com.momid.parser.expression.*

val functionCallParameters by lazy {
    oneOrZero(splitByNW(one(spaces + complexExpression["functionParameter"] + spaces), ","))
}

val functionCall by lazy {
    functionName["functionName"] + insideOf('(', ')') {
        functionCallParameters
    }["functionParameters"] + spaces
}

fun ExpressionResultsHandlerContext.handleFunctionCallParsing(): Result<FunctionCallParsingO> {
    with(this.expressionResult) {
        val functionName = this["functionName"]
        val functionParameters = this["functionParameters"].continuing?.continuing?.asMulti()?.map {
            val functionParameter = it
            functionParameter
        }.orEmpty().also {
            if (it.isEmpty()) {
                println("is empty")
            }
            it.forEach {
                println("parameter " + it.tokens)
            }
        }

        return Ok(FunctionCallParsingO(functionName.parsing, functionParameters.map { it.parsing }, this.parsing))
    }
}

fun ExpressionResultsHandlerContext.handleFunctionCallEvaluating(currentGeneration: CurrentGeneration): Result<FunctionCallEvaluating> {
    val functionCallParsing = handleFunctionCallParsing().okOrReport {
        return it.to()
    }

    val parameterEvaluations = functionCallParsing.functionParameters.map {
        val (evaluation, outputType) = continueWithOne(it.expressionResult, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }
        Evaluation(evaluation, outputType, it)
    }

    return Ok(FunctionCallEvaluating(functionCallParsing.functionName, parameterEvaluations, null, functionCallParsing.parsing))
}

fun ExpressionResultsHandlerContext.handleFunctionCall(currentGeneration: CurrentGeneration, functionReceiver: Eval? = null): Result<Pair<String, OutputType>> {
    val functionCall = handleFunctionCallEvaluating(currentGeneration).okOrReport {
        return it.to()
    }

    if (functionReceiver != null) {
        functionCall.receiver = functionReceiver
    }

    with(functionCall) {
        val functionName = this.name.tokens

        val functionCallEvaluation = when (functionName) {
            "ref" -> continueStraight(this.parsing.expressionResult) { handleReferenceFunction(functionCall, currentGeneration) }
            "print" -> continueStraight(this.parsing.expressionResult) { handlePrintFunction(functionCall, currentGeneration) }
            "sleep" -> continueStraight(this.parsing.expressionResult) { handleSleep(functionCall,  currentGeneration) }
            "initGraphics" -> continueStraight(this.parsing.expressionResult) { handleGraphicsInit(functionCall, currentGeneration) }
            "createWindow" -> continueStraight(this.parsing.expressionResult) { handleCreateWindow(functionCall, currentGeneration) }
            "createRenderer" -> continueStraight(this.parsing.expressionResult) { handleCreateRenderer(functionCall, currentGeneration) }
            "drawLine" -> continueStraight(this.parsing.expressionResult) { handleDrawLine(functionCall, currentGeneration) }
            "update" -> continueStraight(this.parsing.expressionResult) { handleUpdate(functionCall, currentGeneration) }
            else -> {
//                val (function, cFunction) = resolveFunction(functionCall, currentGeneration)
//                    ?: return Error("unresolved function: " + functionCall.name.tokens, this.parsing.range)

                if (isLambdaInvokeFunction(functionCall)) {
                    return handleLambdaInvokeFunction(functionCall, currentGeneration)
                }

                var resolvedFunctions = findMatchingFunctions(functionCall, currentGeneration)

                var resolvedBaseFunctions = resolvedFunctions.filter {
                    if (it.first is GenericFunction) {
                        (it.first as GenericFunction).unsubstituted
                    } else {
                        true
                    }
                }

                if (resolvedBaseFunctions.isEmpty()) {
//                    discoverFunction(functionCall, currentGeneration)
//                    resolvedFunctions = findMatchingFunctions(functionCall, currentGeneration)
//                    resolvedBaseFunctions = resolvedFunctions.filter {
//                        if (it.first is GenericFunction) {
//                            (it.first as GenericFunction).unsubstituted
//                        } else {
//                            true
//                        }
//                    }
                }

                if (resolvedBaseFunctions.isEmpty()) {
                    return Error("unresolved function " + functionCall.parsing.tokens, functionCall.parsing.range)
                }

                if (resolvedBaseFunctions.size > 1) {
                    return Error("multiple functions available with the provided parameters", functionCall.parsing.range)
                }

                val resolvedNonBaseFunctions = resolvedFunctions.filter {
                    if (it.first is GenericFunction) {
                        !(it.first as GenericFunction).unsubstituted
                    } else {
                        false
                    }
                }
                val chosenFunction = if (resolvedNonBaseFunctions.isNotEmpty()) {
                    resolvedNonBaseFunctions[0]
                } else {
                    resolvedBaseFunctions[0]
                }

                var (function, cFunction) = chosenFunction

                if (function == listSetFunction) {
                    return handleListSetFunction(functionCall, currentGeneration)
                } else if (isLambdaInvokeFunction(functionCall)) {
                    return handleLambdaInvokeFunction(functionCall, currentGeneration)
                }

                if (function is GenericFunction) {
                    if (function.unsubstituted) {
                        function = function.clone()
                        functionCall.parameters.forEachIndexed { index, parameter ->
                            val (typesMatch, substitutions) = typesMatch(parameter.outputType, function.parameters[index].type)

                            if (!typesMatch) {
                                return Error(
                                    "type mismatch expected " + function.parameters[index].type + " got " + parameter,
                                    parameter.parsing.range
                                )
                            }
                        }

                        if (function.function is ClassFunction) {
                            val (typesMatch, substitutions) = typesMatch(functionCall.receiver!!.outputType, (function.function as ClassFunction).receiverType)

                            if (!typesMatch) {
                                return Error(
                                    "type mismatch expected " + (function.function as ClassFunction).receiverType + " got " + functionCall.receiver!!.outputType,
                                    functionCall.parsing.range
                                )
                            }
                        }
                    }

                    functionCall.parameters.forEachIndexed { index, parameter ->
                        if (parameter.outputType is EarlyLambda) {
                            val (lambdaEvaluation, lambdaOutputType) = continueWithOne(parameter.parsing.expressionResult, lambda) { handleLambda(currentGeneration, function.parameters[index]) }.okOrReport {
                                return it.to()
                            }

                            if (function.parameters[index].type !is FunctionType) {
                                return Error("type mismatch. expected " + function.parameters[index].type.text + " found " + lambdaOutputType.text, this.parsing.range)
                            }

                            val lambdaReturnType = (lambdaOutputType as FunctionType).outputFunction.returnType
                            val functionParameterLambdaReturnType = (function.parameters[index].type as FunctionType).outputFunction.returnType

                            val (typesMatch, substitutions) = typesMatch(lambdaReturnType, functionParameterLambdaReturnType)
                            if (!typesMatch) {
                                return Error("lambda should return " + functionParameterLambdaReturnType.text + " but returns " + lambdaReturnType.text, this.parsing.range)
                            }

                            functionCall.parameters[index].cEvaluation = lambdaEvaluation
                        }
                    }

                    val resolvedCFunction = createGenericFunctionIfNotExists(currentGeneration, function).okOrReport {
                        return it.to()
                    }

                    return Ok(
                        Pair(
                            cFunctionCall(resolvedCFunction.name, (functionCall.parameters.mapIndexed { index, evaluation ->
                                if (function.parameters[index].isReferenceParameter) {
                                    "&(" + evaluation.cEvaluation + ")"
                                } else {
                                    evaluation.cEvaluation
                                }
                            } as ArrayList).apply {
                                if (functionReceiver != null) {
                                    this.add(0, functionReceiver.cEvaluation)
                                }
                            }),
                            function.returnType
                        )
                    )
                } else {
                    functionCall.parameters.forEachIndexed { index, parameter ->
                        if (parameter.outputType is EarlyLambda) {
                            val (lambdaEvaluation, lambdaOutputType) = continueWithOne(parameter.parsing.expressionResult, lambda) { handleLambda(currentGeneration, function.parameters[index]) }.okOrReport {
                                return it.to()
                            }

                            if (function.parameters[index].type !is FunctionType) {
                                return Error("type mismatch. expected " + function.parameters[index].type.text + " found " + lambdaOutputType.text, this.parsing.range)
                            }

                            val lambdaReturnType = (lambdaOutputType as FunctionType).outputFunction.returnType
                            val functionParameterLambdaReturnType = (function.parameters[index].type as FunctionType).outputFunction.returnType

                            val (typesMatch, substitutions) = typesMatch(lambdaReturnType, functionParameterLambdaReturnType)
                            if (!typesMatch) {
                                return Error("lambda should return " + functionParameterLambdaReturnType.text + " but returns " + lambdaReturnType.text, this.parsing.range)
                            }

                            functionCall.parameters[index].cEvaluation = lambdaEvaluation
                        }
                    }

                    if (cFunction == null) {
                        throw (Throwable("function is not a generic function and so c function should not have been null"))
                    }
                    return Ok(
                        Pair(
                            cFunctionCall(cFunction.name, (functionCall.parameters.mapIndexed { index, evaluation ->
                                if (function.parameters[index].isReferenceParameter) {
                                    "&(" + evaluation.cEvaluation + ")"
                                } else {
                                    evaluation.cEvaluation
                                }
                            } as ArrayList).apply {
                                if (functionReceiver != null) {
                                    this.add(0, functionReceiver.cEvaluation)
                                }
                            }),
                            function.returnType
                        )
                    )
                }
            }
        }
        return functionCallEvaluation
    }
}

class FunctionCallParsingO(val functionName: Parsing, val functionParameters: List<Parsing>, val parsing: Parsing)

class FunctionCallEvaluating(val name: Parsing, val parameters: List<Evaluation>, var receiver: Eval?, val parsing: Parsing)

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "someFunction(anotherFunction(someParameter0, someParameter1, someParameter3), parameter1, parameter3)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(functionCall))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println(this.tokens.joinToString(""))
            handleFunctionCall(currentGeneration)
        }
    }
}
