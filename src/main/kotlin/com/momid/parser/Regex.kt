package com.momid.parser

import com.momid.parser.expression.*

fun match(expression: Expression, tokens: List<Char>): List<ExpressionResult> {

    val matches = ArrayList<ExpressionResult>()
    var tokenIndex = 0

    while (true) {
        val nextMatch = evaluateExpressionValueic(expression, tokenIndex, tokens)
        if (nextMatch == null) {
            tokenIndex += 1
        } else {
            matches.add(nextMatch)
            val nextIndex = nextMatch.range.last
            tokenIndex = nextIndex
        }
        if (tokenIndex >= tokens.size) {
            break
        }
    }

    return matches
}

fun main() {

    println("")

    val text = "hello! my friend. how are you today ?"
//    val expression = ExactExpression("hello")
    val sideExpression = ConditionExpression { it != 'a' }
    sideExpression.isValueic = true
    sideExpression.name = "side expression"
    val expression = MultiExpression(arrayListOf(sideExpression, ExactExpression("friend"), sideExpression))
    val matches = match(expression, text.toList())
    matches.forEach {
        printExpressionResult(it)
    }
}

fun printExpressionResult(expressionResult: ExpressionResult, indents: Int = 0) {
    when (expressionResult) {
        is MultiExpressionResult -> {
            println(expressionResult.mainExpressionResult.expression.name() + "   " + expressionResult.mainExpressionResult.range)
            if (expressionResult.isNotEmpty()) {
                println(nIndent(3 * (indents + 1)) + "sub expression results of " + expressionResult.mainExpressionResult.expression.name() + " :")
            }
            expressionResult.forEach {
                printExpressionResult(it, indents + 1)
            }
        }
        else -> {
            println(nIndent(3 * indents) + expressionResult.expression.name() + "   " + expressionResult.range)
        }
    }
}

fun nIndent(numberOfIndents: Int): String {
    var indent = ""
    for (indentIndex in 0..numberOfIndents) {
        indent += " "
    }
    return indent
}

fun Expression.name(): String {
    return this.name ?: this.toString()
}
