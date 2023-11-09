package com.momid.parser

import com.momid.parser.expression.*

inline fun <T : Expression> ExpressionResult.isOf(expression: T, then: (ExpressionResult) -> Unit) {
    if (this.expression == expression) {
        then(this)
    }
}

val whileExpression =
    spaced {
        !"while" + "(" + insideParentheses["expression"] + ")" + "{" + insideOfParentheses('{', '}')["codeBlock"] + "}"
    }

operator fun String.not(): ExactExpression {
    return exact(this)
}

fun main() {

    val whileText = ("while (some() && true) {" +
            "while (someOther() && true) {" +
            "weAreInsideAnotherWhile()" +
            "}" +
            "weAreInsideWhile()" +
            "}").toList()

    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(whileExpression))
    val expressionResults = finder.start(whileText)
    expressionResults.forEach {
        it.isOf(whileExpression) {
            println(it["expression"].range)
        }
        printExpressionResult(it)
    }
    println(expressionResults.size)
}
