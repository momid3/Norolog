package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.parser.expression.*
import com.momid.parser.not

val functionName =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })

val functionParameter =
    ignoreParentheses(condition { it != ',' && it != ')' })

val functionParameters =
    anyOf(inline(inline(some(functionParameter["parameter"] + spaces + "," + spaces)) + functionParameter["parameter"]), functionParameter["parameter"])

val function =
    functionName["functionName"] + !"(" + insideParentheses["parameters"] + !")"

fun ExpressionResult.isOf(name: String, then: (ExpressionResult) -> Unit) {
    if (this.expression.name == name) {
        then(this)
    }
}

/***
 * applies the condition until it is false and skips any parentheses and the content of them in between
 */
fun ignoreParentheses(conditionExpression: ConditionExpression): CustomExpression {
    return CustomExpression { tokens, startIndex, endIndex ->
        var tokenEndIndex = startIndex
        while (true) {
            if (tokenEndIndex >= endIndex) {
                break
            }
            if (tokens[tokenEndIndex] == '(') {
                val nextParenthesesIndex = (evaluateExpressionValueic(insideParentheses, tokenEndIndex + 1, tokens, endIndex)?.range?.last) ?: return@CustomExpression -1
                if (nextParenthesesIndex <= endIndex - 1) {
                    tokenEndIndex = nextParenthesesIndex + 1
                } else {
                    tokenEndIndex = nextParenthesesIndex
                    break
                }
            }
            tokenEndIndex = evaluateExpressionValueic(conditionExpression, tokenEndIndex, tokens, endIndex)?.range?.last ?: break
            if (tokenEndIndex >= endIndex) {
                break
            }
        }
        return@CustomExpression tokenEndIndex
    }
}

fun ExpressionResult.subs(name: String): List<ExpressionResult> {
    if (this is MultiExpressionResult) {
        return this.filter {
            it.expression.name == name
        }
    } else {
        if (this.expression.name == name) {
            return listOf(this)
        } else {
            return emptyList()
        }
    }
}

fun inline(multiExpression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        val inlinedExpressionResults = ArrayList<ExpressionResult>()
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        if (expressionResult !is MultiExpressionResult) {
            throw(Throwable("expression " + multiExpression.text() + " does not evaluate to a MultiExpressionResult"))
        }
        expressionResult.forEach {
            if (it is MultiExpressionResult) {
                if (it.expression.name == null) {
                    inlinedExpressionResults.addAll(it.expressionResults)
                } else {
                    inlinedExpressionResults.add(it)
                }
            } else {
                inlinedExpressionResults.add(it)
            }
        }
        expressionResult.expressionResults.clear()
        expressionResult.expressionResults.addAll(inlinedExpressionResults)
        return@CustomExpressionValueic expressionResult
    }
}

fun inlineContent(multiExpression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        if (expressionResult !is ContentExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a ContentExpressionResult"))
        }
        return@CustomExpressionValueic expressionResult.content
    }
}

fun inlineToOne(multiExpression: Expression): CustomExpressionValueic {
    return inlineContent(CustomExpressionValueic { tokens, startIndex, endIndex ->
        val inlinedExpressionResults = ArrayList<ExpressionResult>()
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        if (expressionResult !is MultiExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a MultiExpressionResult"))
        }
        expressionResult.forEach {
                if (it.expression.name != null) {
                    inlinedExpressionResults.add(it)
                }
            }

        if (inlinedExpressionResults.size > 1) {
            throw(Throwable("expression " + expressionResult.correspondingTokensText(tokens) + (multiExpression.name) + (multiExpression::class.simpleName) + "is not inlinable to one, because it has more than one named sub expressions"))
        }
        if (inlinedExpressionResults.isEmpty()) {
            throw(Throwable("expression " + expressionResult.correspondingTokensText(tokens) + (multiExpression.name) + (multiExpression::class.simpleName) + "is not inlinable to one, because it has no named sub expressions"))
        }

        return@CustomExpressionValueic ContentExpressionResult(inlinedExpressionResults[0].apply { this.range = expressionResult.range }, inlinedExpressionResults[0])
    })
}

//fun ExpressionResultsHandlerContext.handleFunction(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
//    var output = ""
//    with(this.expressionResult) {
//        isOf(function) {
//            println("function: " + it.tokens)
//            println("function parameters are: " + it["parameters"].tokens())
//
//            val parametersEvaluation = continueWithOne(it["parameters"], functionParameters) { handleFunctionCallParameters(currentGeneration) }
//            val functionName = it["functionName"].tokens()
//
//            if (parametersEvaluation is Error<*>) {
//                currentGeneration.errors.add(parametersEvaluation)
//                println(parametersEvaluation.error)
//                return Error(parametersEvaluation.error, parametersEvaluation.range)
//            }
//            if (parametersEvaluation is Ok) {
//                val parameters = parametersEvaluation.ok
//
//                val functionCall = FunctionCallEvaluation(functionName, parameters)
//
//                val functionCallEvaluation = when (functionName) {
//                    "ref" -> continueStraight(it) { handleReferenceFunction(functionCall, currentGeneration) }
//                    "print" -> continueStraight(it) { handlePrintFunction(functionCall, currentGeneration) }
//                    "initGraphics" -> continueStraight(it) { handleGraphicsInit(functionCall, currentGeneration) }
//                    else -> {
//                        val (function, cFunction) = resolveFunction(functionCall, currentGeneration)
//                            ?: return Error("unresolved function: " + functionCall.name, it.range)
//
//                        return Ok(
//                            Pair(
//                                cFunctionCall(cFunction.name, functionCall.parameters.map { it.first }),
//                                function.returnType
//                            )
//                        )
//                    }
//                }
//
//                return functionCallEvaluation
//            }
//        }
//        return Error("is not a function", this.range)
//    }
//}

/***
 * @return a list of evaluated function parameters. first element is the evaluated output of the parameter
 * and last element is the type of the evaluated value.
 */
fun ExpressionResultsHandlerContext.handleFunctionCallParameters(currentGeneration: CurrentGeneration): Result<List<Pair<String, OutputType>>> {
    this.expressionResult.isOf(functionParameters) {
        print("function parameters:", it)
        val parameters = ArrayList<Pair<String, OutputType>>()
        it.content.subs("parameter").forEach {
            print("function parameter:", it)
            val parameterEvaluation = continueWithOne(it, complexExpression) { handleComplexExpression(currentGeneration) }

            if (parameterEvaluation is Error<*>) {
                currentGeneration.errors.add(parameterEvaluation)
                println(parameterEvaluation.error)
                return Error(parameterEvaluation.error, parameterEvaluation.range)
            }
            if (parameterEvaluation is Ok<Pair<String, OutputType>>) {
                parameters.add(parameterEvaluation.ok)
            }
        }
        return Ok(parameters)
    }
    return Error("is not function parameters", this.expressionResult.range)
}

inline fun <T> List<T>.forEveryIndexed(onEach: (index: Int, item: T) -> Boolean): Boolean {
    this.forEachIndexed { index, item ->
        if (!onEach(index, item)) {
            return false
        }
    }
    return true
}

fun functionSignaturesMatch(function: Function, anotherFunction: FunctionCallEvaluating): Boolean {
    return function.name == anotherFunction.name.tokens && function.parameters.forEveryIndexed { index, parameter ->
        anotherFunction.parameters[index].outputType == parameter.type
    } && if (function is ClassFunction) {
        function.receiverType == anotherFunction.receiver!!.outputType
    } else {
        true
    }
}

fun resolveFunction(function: FunctionCallEvaluating, currentGeneration: CurrentGeneration): Pair<Function, CFunction>? {
    currentGeneration.functionsInformation.functionsInformation.forEach { (functionDeclaration, cFunctionDeclaration) ->
        if (functionSignaturesMatch(functionDeclaration, function)) {
            return Pair(functionDeclaration, cFunctionDeclaration)
        }
    }
    return null
}

fun ExpressionResultsHandlerContext.handlePrintFunction(functionCall: FunctionCallEvaluating, currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        var output = ""
        if (functionCall.parameters.size > 1) {
            print("parameters of more than one are not currently supported for print function")
            return Error("parameters of more than one are not currently supported for print function", this.range)
        } else {
            with(functionCall.parameters[0]) {

                println("printing: " + this.cEvaluation)
                println("its type is: " + this.outputType)

                val parameterType = this.outputType

                when (parameterType) {
                    outputStringType -> {
                        output += "printf" + "(" + this.cEvaluation + ")"
                        return Ok(Pair(output, norType))
                    }

                    outputIntType -> {
                        output += "printf" + "(" + "\"%d\\n\"" + ", " + this.cEvaluation + ")"
                        return Ok(Pair(output, norType))
                    }

                    outputBooleanType -> {
                        output += "printf" + "(" + "\"%d\\n\"" + ", " + this.cEvaluation + ")"
                        return Ok(Pair(output, norType))
                    }

                    else -> {
                        return Error("this variable type could not be printed: " + parameterType, this@handlePrintFunction.expressionResult.range)
                    }
                }
            }
        }
    }
}

class FunctionCallParsing(val name: String, val parameters: List<String>)

class FunctionCallEvaluation(val name: String, val parameters: List<Pair<String, OutputType>>)

fun main() {
//    val text = "someFunction(3 + 7 + anotherFunction() + (someVar + 373), 7, 3)".toList()
//    val finder = ExpressionFinder()
//    finder.registerExpressions(listOf(function))
//    val expressionResults = finder.start(text)

    val currentGeneration = CurrentGeneration()
    val text = "((theVal.someVariable + 7));".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(ignoreParentheses(condition { it != ',' && it != ')' })))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
//            handleClassInitialization(currentGeneration)
            println(this.expressionResult.tokens())
            return@handleExpressionResult Ok("")
        }
    }
}
