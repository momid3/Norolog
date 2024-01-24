package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

fun splitBy(expression: Expression, splitBy: String): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        var nextTokenIndex = startIndex
        val subExpressionResults = ArrayList<ExpressionResult>()
        val splitExpression = ExactExpression(splitBy)
        val firstEvaluation = evaluateExpressionValueic(wanting(expression, splitExpression), startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        nextTokenIndex = firstEvaluation.nextTokenIndex
        subExpressionResults.add(firstEvaluation)
        while (true) {
            val splitEvaluation = evaluateExpressionValueic(ExactExpression(splitBy), nextTokenIndex, tokens, endIndex) ?: break
            nextTokenIndex = splitEvaluation.nextTokenIndex
            val nextEvaluation = evaluateExpressionValueic(wanting(expression, splitExpression), nextTokenIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
            nextTokenIndex = nextEvaluation.nextTokenIndex
            subExpressionResults.add(nextEvaluation)
        }
        return@CustomExpressionValueic MultiExpressionResult(ExpressionResult(expression, startIndex..nextTokenIndex), subExpressionResults)
    }
}

/***
 * splitBy without "wanting" for each element
 */
fun splitByNW(expression: Expression, splitBy: String): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        var nextTokenIndex = startIndex
        val subExpressionResults = ArrayList<ExpressionResult>()
        val splitExpression = ExactExpression(splitBy)
        val firstEvaluation = evaluateExpressionValueic(expression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        nextTokenIndex = firstEvaluation.nextTokenIndex
        subExpressionResults.add(firstEvaluation)
        while (true) {
            val splitEvaluation = evaluateExpressionValueic(ExactExpression(splitBy), nextTokenIndex, tokens, endIndex) ?: break
            nextTokenIndex = splitEvaluation.nextTokenIndex
            val nextEvaluation = evaluateExpressionValueic(expression, nextTokenIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
            nextTokenIndex = nextEvaluation.nextTokenIndex
            subExpressionResults.add(nextEvaluation)
        }
        return@CustomExpressionValueic MultiExpressionResult(ExpressionResult(expression, startIndex..nextTokenIndex), subExpressionResults)
    }
}

fun main() {
    val expression =
        splitBy(className["o"] + !":" + className, ",")
    val currentGeneration = CurrentGeneration()
    val text = "hi:hello,ooo:ooo,ooooooo:oooooooo".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(expression))
    finder.start(text).forEach {
        handleExpressionResult<Boolean>(finder, it, text) {
            this.expressionResult.forEach {
                println(it is ContinueExpressionResult)
                println(it.continuing?.tokens())
            }
            return@handleExpressionResult Ok(true)
        }
    }

//    val expression =
//        wanting(!"hello", !",")
//    val currentGeneration = CurrentGeneration()
//    val text = "hello,oooooo".toList()
//    val finder = ExpressionFinder()
//    finder.registerExpressions(listOf(expression))
//    finder.start(text).forEach {
//        handleExpressionResult<Boolean>(finder, it, text) {
//            println(this.expressionResult.continuing?.tokens())
//            return@handleExpressionResult Ok(true)
//        }
//    }
}
