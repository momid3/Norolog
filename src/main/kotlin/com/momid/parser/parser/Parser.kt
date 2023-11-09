package com.momid.parser.parser

import com.momid.parser.expression.*
import com.momid.parser.not

val variableName =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })

val number =
    condition { it.isDigit() } + some0(condition { it.isDigit() })

val atomicExp =
    anyOf(variableName, number)

val operator =
    anyOf('+', '-', '*', '/')

val simpleExpression =
    some(anyOf(spaces + atomicExp + spaces, spaces + operator + spaces))

val expressionInParentheses =
    insideParentheses

val simpleExpressionInParentheses =
    !"(" + expressionInParentheses["insideParentheses"] + ")"

val complexExpression =
    some(anyOf(simpleExpression, simpleExpressionInParentheses))

//private fun handleExpressionResults(expressionFinder: ExpressionFinder, expressionResult: ExpressionResult, tokens: List<Char>) {
//    handleExpressionResult(expressionFinder, expressionResult, tokens) {
//        this.expressionResult.isOf(complexExpression) {
//            println("complex expression: " + it.correspondingTokensText(this.tokens))
//            it.forEach {
//                it.isOf(simpleExpression) {
//                    println("simple: " + it.correspondingTokensText(this.tokens))
//                }
//                it.isOf(simpleExpressionInParentheses) {
//                    println("simple in parentheses: " + it.correspondingTokensText(this.tokens))
////                       handleExpressionResults(
////                           expressionFinder,
////                           expressionFinder.start(tokens, it["insideParentheses"].range),
////                           tokens
////                       )
//                    continueWith(it["insideParentheses"])
//                }
//            }
//        }
//    }
//}

fun main() {

    val text = "someVar + 3 + 7 + (3 + 37 + (373 + 373))".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(complexExpression))
    val expressionResults = finder.start(text)
//    expressionResults.forEach {
//        handleExpressionResults(finder, it, text)
//    }
}
