package com.momid.compiler

import com.momid.parser.expression.*

val functionName =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })

val functionParameter =
    ignoreParentheses(condition { it != ',' && it != ')' })

val functionParameters =
    anyOf(inline(inline(some(functionParameter["parameter"] + spaces + "," + spaces)) + functionParameter["parameter"]), functionParameter["parameter"])

val function =
    functionName + "(" + insideParentheses["parameters"] + ")"

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
            if (tokenEndIndex >= tokens.size) {
                break
            }
            tokenEndIndex = evaluateExpressionValueic(conditionExpression, tokenEndIndex, tokens)?.range?.last ?: break
            if (tokens[tokenEndIndex] == '(') {
                val nextParenthesesIndex = (evaluateExpressionValueic(insideParentheses, tokenEndIndex + 1, tokens)?.range?.last) ?: return@CustomExpression -1
                if (nextParenthesesIndex <= tokens.lastIndex) {
                    tokenEndIndex = nextParenthesesIndex + 1
                } else {
                    tokenEndIndex = nextParenthesesIndex
                    break
                }
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
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens) ?: return@CustomExpressionValueic null
        if (expressionResult !is MultiExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a MultiExpressionResult"))
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
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens) ?: return@CustomExpressionValueic null
        if (expressionResult !is ContentExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a ContentExpressionResult"))
        }
        return@CustomExpressionValueic expressionResult.content
    }
}

fun inlineToOne(multiExpression: Expression): CustomExpressionValueic {
    return inlineContent(CustomExpressionValueic { tokens, startIndex, endIndex ->
        val inlinedExpressionResults = ArrayList<ExpressionResult>()
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens) ?: return@CustomExpressionValueic null
        if (expressionResult !is MultiExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a MultiExpressionResult"))
        }
        expressionResult.forEach {
                if (it.expression.name != null) {
                    inlinedExpressionResults.add(it)
                }
            }

        if (inlinedExpressionResults.size > 1) {
            throw(Throwable("expression " + ((multiExpression.name) ?: (multiExpression::class.simpleName)) + "is not inlinable to one, because it has more than one named sub expressions"))
        }
        if (inlinedExpressionResults.isEmpty()) {
            throw(Throwable("expression " + ((multiExpression.name) ?: (multiExpression::class.simpleName)) + "is not inlinable to one, because it has no named sub expressions"))
        }

        return@CustomExpressionValueic ContentExpressionResult(inlinedExpressionResults[0].apply { this.range = expressionResult.range }, inlinedExpressionResults[0])
    })
}

fun ExpressionResultsHandlerContext<String>.handleFunction(currentGeneration: CurrentGeneration): Result<String> {
    var output = ""
    with(this.expressionResult) {
        isOf(function) {
            println("function: " + it.correspondingTokensText(tokens))
            val parametersEvaluation = continueWithOne(it["parameters"], functionParameters) { handleFunctionCallParameters(currentGeneration) }

            if (parametersEvaluation is Error<*>) {
                currentGeneration.errors.add(parametersEvaluation)
                println(parametersEvaluation.error)
                return Error(parametersEvaluation.error, parametersEvaluation.range)
            }
            if (parametersEvaluation is Ok<List<String>>) {
                val parameters = parametersEvaluation.ok

                if (parameters.size > 1) {
                    print("parameters of more than one are not currently supported for functions")
                    return Error("parameters of more than one are not currently supported for functions", this.range)
                } else {
                    output += "printf" + "(" + "\"%d\\n\"" + ", " + parameters[0] + ")" + ";" + "\n"
                    currentGeneration.generatedSource += output
                    return Ok("")
                }
            }
        }
        return Error("is not a function", this.range)
    }
}

fun ExpressionResultsHandlerContext<String>.handleFunctionCallParameters(currentGeneration: CurrentGeneration): Result<List<String>> {
    this.expressionResult.isOf(functionParameters) {
        print("function parameters:", it)
        val parameters = ArrayList<String>()
        it.content.subs("parameter").forEach {
            print("function parameter:", it)
            val parameterEvaluation = continueWithOne(it, complexExpression) { handleComplexExpression(currentGeneration) }

            if (parameterEvaluation is Error<*>) {
                currentGeneration.errors.add(parameterEvaluation)
                println(parameterEvaluation.error)
                return Error(parameterEvaluation.error, parameterEvaluation.range)
            }
            if (parameterEvaluation is Ok<String>) {
                parameters.add(parameterEvaluation.ok)
            }
        }
        return Ok(parameters)
    }
    return Error("is not function parameters", this.expressionResult.range)
}

fun main() {
    val text = "someFunction(3 + 7 + anotherFunction() + (someVar + 373), 7, 3)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(function))
    val expressionResults = finder.start(text)

//    expressionResults.forEach {
//        handleExpressionResult(finder, it, text) {
//            with(this.expressionResult) {
//                isOf(function) {
//                    println("function: " + it.correspondingTokensText(tokens))
////                println(it["parameters"].correspondingTokensText(text))
//                    continueWith<String>(it["parameters"], functionParameters)
//                }
//                isOf(functionParameters) {
//                    print("function parameters:", it)
//                    it.content.subs("parameter").forEach {
//                            print("function parameter:", it)
//                            continueWith(it, complexExpression) { handleComplexExpression() }
//                    }
//                }
////                isOf("parameters") {
////                    print("function parameters:", it)
//////                    it.subs("parameter").forEach {
//////                        print("function parameter:", it)
//////                        continueWith(it, complexExpression)
//////                    }
////                    println(it.expression.name)
////                    it.forEach {
////                        println(it.expression.name)
////                        if (it is MultiExpressionResult) {
////                            it.forEach {
////                                println(it.expression.name)
////                                if (it is MultiExpressionResult) {
////                                    it.forEach {
////                                        println(it.expression.name)
////                                    }
////                                }
////                            }
////                            println()
////                        }
////                    }
////                    println()
////                }
//            }
//            return@handleExpressionResult Ok("")
//        }
//    }
}
