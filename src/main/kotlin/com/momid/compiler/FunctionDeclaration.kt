package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.parser.expression.*
import com.momid.parser.not

val anything =
    some0(condition { true })

val functionDeclarationParameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val fdp = functionDeclarationParameter

val functionDeclarationParameters =
    oneOrZero(
        splitBy(fdp, ",")
    )

val functionDeclaration =
    !"fun" + space + functionName["functionName"] + insideOf('(', ')') {
        functionDeclarationParameters["functionDeclarationParameters"]
    } + spaces + oneOrZero(one(spaces + !":" + spaces + outputTypeO["functionReturnType"] + spaces), "functionReturnType") + spaces +
            insideOf('{', '}') {
        anything["functionInside"]
    }

fun ExpressionResultsHandlerContext.handleFunctionDeclarationParsing(currentGeneration: CurrentGeneration): Result<FunctionDeclarationParsing> {
    this.expressionResult.isOf(functionDeclaration) {
        val functionName = parsing(it["functionName"])
        val fdps = it["functionDeclarationParameters"].continuing?.continuing

        val functionParameters = fdps?.asMulti()?.map {
            println("function parameter: " + it.tokens())
            val fdp = it.continuing {
                println("expected function parameter, got: " + it.tokens())
                return Error("expected function parameters, got: " + it.tokens(), it.range)
            }
            FunctionParameterParsing(parsing(fdp["parameterName"]), parsing(fdp["parameterType"]))
        }

        val returnType = with(it["functionReturnType"].continuing) {
            if (this != null) {
                parsing(this)
            } else {
                null
            }
        }
        val bodyRange = it["functionInside"].continuing!!.range

        return Ok(FunctionDeclarationParsing(functionName, functionParameters, returnType, bodyRange))
    }
    return Error("is not function declaration", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleFunctionDeclaration(
    currentGeneration: CurrentGeneration
): Result<Boolean> {
    val functionDeclaration = continueStraight(this.expressionResult) { handleFunctionDeclarationParsing(currentGeneration) }.okOrReport {
        println(it.error)
        return it.to()
    }

    with(functionDeclaration) {
        val returnType = if (this.returnType != null) {
            continueStraight(this.returnType.expressionResult!!) { handleOutputType(currentGeneration) }.okOrReport {
                println(it.error)
                return it.to()
            }
        } else {
            norType
        }

        val function = Function(this.name.tokens, this.parameters?.map {
            FunctionParameter(it.name.tokens, continueWithOne(it.outputType.expressionResult!!, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                println(it.error)
                return it.to()
            })
        } ?: emptyList(), returnType, this.bodyRange)

        val cFunctionParameters = function.parameters.map {
            val cType = resolveType(it.type, currentGeneration)
            CFunctionParameter(createVariableName(), cType)
        }

        val functionScope = Scope()
        functionScope.scopeContext = FunctionContext(function)

        function.parameters.forEachIndexed { index, functionParameter ->
            val variableInformation = VariableInformation(
                cFunctionParameters[index].name,
                cFunctionParameters[index].type,
                "",
                functionParameter.name,
                functionParameter.type
            )
            functionScope.variables.add(variableInformation)
        }

        val cFunctionName = currentGeneration.createCFunctionName()

        val cFunctionReturnType = resolveType(returnType, currentGeneration)

        val cFunction = CFunction(cFunctionName, cFunctionParameters, cFunctionReturnType, "")

        currentGeneration.functionsInformation.functionsInformation[function] = cFunction

        val cFunctionBody = continueStraight(ExpressionResult(this@handleFunctionDeclaration.expressionResult.expression, this.bodyRange)) { handleCodeBlock(currentGeneration, functionScope) }.okOrReport {
            println(it.error)
            return it.to()
        }

        currentGeneration.functionDeclarationsGeneratedSource += cFunction(cFunctionName, cFunctionParameters.map { cTypeAndVariableName(it.type, it.name) }, cTypeName(cFunctionReturnType), cFunctionBody.trim()) + "\n"

        return Ok(true)
    }
}

fun ExpressionResultsHandlerContext.parsing(expressionResult: ExpressionResult): Parsing {
    return Parsing(expressionResult.tokens(), expressionResult.range, expressionResult)
}

class FunctionParameterParsing(val name: ParsingElement, val outputType: ParsingElement)

class FunctionDeclarationParsing(
    val name: ParsingElement,
    val parameters: List<FunctionParameterParsing>?,
    val returnType: ParsingElement?,
    val bodyRange: IntRange
)

fun oneOrZero(expression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        val evaluation = evaluateExpressionValueic(some0(expression), startIndex, tokens, endIndex)
        if (evaluation is MultiExpressionResult) {
            if (evaluation.size > 1) {
                return@CustomExpressionValueic null
            }
            if (evaluation.isEmpty()) {
                return@CustomExpressionValueic ContinueExpressionResult(evaluation, null)
            } else {
                return@CustomExpressionValueic ContinueExpressionResult(evaluation, evaluation.expressionResults[0])
            }
        } else {
            throw (Throwable("some0 should have returned MultiExpressionResult but returned something else"))
        }
    }
}

fun oneOrZero(expression: Expression, name: String): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        val evaluation = evaluateExpressionValueic(some0(expression)[name], startIndex, tokens, endIndex)
        if (evaluation is MultiExpressionResult) {
            if (evaluation.size > 1) {
                return@CustomExpressionValueic null
            }
            if (evaluation.isEmpty()) {
                return@CustomExpressionValueic ContinueExpressionResult(evaluation, null)
            } else {
                return@CustomExpressionValueic ContinueExpressionResult(evaluation, evaluation.expressionResults[0])
            }
        } else {
            throw (Throwable("some0 should have returned MultiExpressionResult but returned something else"))
        }
    }
}

fun one(expression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        val evaluation = evaluateExpressionValueic(expression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        var namedExpressionResult: ExpressionResult? = null
        if (evaluation is MultiExpressionResult) {
            evaluation.forEach {
                if (it.expression.name != null) {
                    if (namedExpressionResult == null) {
                        namedExpressionResult = it
                    } else {
                        throw (Throwable("there should be only one named expression"))
                    }
                }
            }
            if (namedExpressionResult == null) {
                throw (Throwable("there should be one named expression but yours has none"))
            } else {
                return@CustomExpressionValueic namedExpressionResult!!.apply { this.nextTokenIndex = evaluation.nextTokenIndex }
            }
        } else {
            evaluation
        }
    }
}

val ExpressionResult.continuing: ExpressionResult?
    get() {
        if (this is ContinueExpressionResult) {
            return this.content
        } else {
            throw (Throwable("this expression result is not a ContinueExpressionResult"))
        }
    }
