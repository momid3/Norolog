package com.momid.parser.expression

import com.momid.compiler.text
import com.momid.parser.nIndent

fun ExpressionResult.correspondingTokens(tokens: List<Char>): List<Char> {
    return tokens.slice(this.range.first until this.range.last)
}

fun ExpressionResult.correspondingTokensText(tokens: List<Char>): String {
    return tokens.slice(this.range.first until this.range.last).joinToString("")
}

fun <T> handleExpressionResult(
    expressionFinder: ExpressionFinder,
    expressionResult: ExpressionResult,
    tokens: List<Char>,
    handle: ExpressionResultsHandlerContext.() -> Result<T>
): Result<T> {
    return ExpressionResultsHandlerContext(expressionFinder, expressionResult, tokens, handle).handle()
}

typealias Parsing = ParsingElement

class ParsingElement(val tokens: String, val range: IntRange, val expressionResult: ExpressionResult)

val ExpressionResult.content: ExpressionResult
    get() {
        if (this is ContentExpressionResult) {
            return this.content
        } else {
            throw(Throwable("expression is not a content expression"))
        }
    }

inline fun ExpressionResult.continuing(report: (ExpressionResult) -> Unit): ExpressionResult {
    if (this is ContinueExpressionResult) {
        if (this.content != null) {
            return this.content
        } else {
            report(this)
        }
        throw (Throwable("you should have returned in the report function"))
    } else {
        throw (Throwable("this expression kind does return a ContinueExpressionResult"))
    }
}

class ExpressionResultsHandlerContext(
    val expressionFinder: ExpressionFinder,
    val expressionResult: ExpressionResult,
    val tokens: List<Char>,
    val handle: ExpressionResultsHandlerContext.() -> Result<*>
) {
    fun continueWith(expressionResult: ExpressionResult, anotherHandler: ExpressionResultsHandlerContext.() -> Result<*>) {
        expressionFinder.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, anotherHandler).anotherHandler()
        }
    }

    fun continueWith(expressionResult: ExpressionResult) {
        expressionFinder.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, handle).handle()
        }
    }

    fun continueWith(expressionResult: ExpressionResult, vararg registerExpressions: Expression, anotherHandler: ExpressionResultsHandlerContext.() -> Result<*>) {
        val finder: ExpressionFinder
        ExpressionFinder().apply {
            finder = this
            registerExpressions(registerExpressions.toList())
        }.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(finder, it, tokens, anotherHandler).anotherHandler()
        }
    }

    fun continueWith(expressionResult: ExpressionResult, vararg registerExpressions: Expression) {
        ExpressionFinder().apply { registerExpressions(registerExpressions.toList()) }.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, handle).handle()
        }
    }

    fun <R> continueWithOne(expressionResult: ExpressionResult, vararg registerExpressions: Expression, anotherHandler: ExpressionResultsHandlerContext.() -> Result<R>): Result<R> {
        val finder: ExpressionFinder
        ExpressionFinder().apply {
            finder = this
            registerExpressions(registerExpressions.toList())
        }.start(tokens, expressionResult.range).apply {
            if (isNotEmpty()) {
                return ExpressionResultsHandlerContext(finder, this[0], tokens, anotherHandler).anotherHandler()
            } else {
                return NoExpressionResultsError(expressionResult.range)
            }
        }
    }

    fun <R> continueWithOneSeparate(expressionResult: ExpressionResult, vararg registerExpressions: Expression, anotherHandler: ExpressionResultsHandlerContext.() -> Result<R>): Result<R> {
        val finder: ExpressionFinder
        ExpressionFinder().apply {
            finder = this
            registerExpressions(registerExpressions.toList())
        }.start((expressionResult.tokens()).toList()).apply {
            if (isNotEmpty()) {
                return ExpressionResultsHandlerContext(finder, this[0], (expressionResult.tokens()).toList(), anotherHandler).anotherHandler()
            } else {
                return NoExpressionResultsError(expressionResult.range)
            }
        }
    }

//    inline fun require(expressionResult: ExpressionResult, registerExpressions: List<Expression>, notFound: (range: IntRange) -> Unit, continueWith: (ExpressionResult) -> Unit) {
//        ExpressionFinder().apply { registerExpressions(registerExpressions) }.start(tokens, expressionResult.range).apply {
//            if (isNotEmpty()) {
//                continueWith(this[0])
//            } else {
//                notFound(expressionResult.range)
//            }
//        }
//    }

    inline fun require(expressionResult: ExpressionResult, registerExpression: Expression, notFound: () -> Unit, continueWith: (ExpressionResult) -> Unit) {
        ExpressionFinder().apply { registerExpressions(listOf(registerExpression)) }.start(tokens, expressionResult.range).apply {
            if (isNotEmpty()) {
                continueWith(this[0])
            } else {
                notFound()
            }
        }
    }

    fun <R> continueStraight(
        expressionResult: ExpressionResult,
        anotherHandler: ExpressionResultsHandlerContext.() -> Result<R>
    ): Result<R> {
        return ExpressionResultsHandlerContext(this.expressionFinder, expressionResult, this.tokens, handle).anotherHandler()
    }

    val ExpressionResult.parsing: Parsing
        get() {
            return Parsing(this.tokens(), this.range, this)
        }

    val ExpressionResult.tokens: String
        get() {
            return this.tokens()
        }

    fun print(expressionResult: ExpressionResult) {
        println(expressionResult.correspondingTokensText(tokens))
    }

    fun print(prefix: String, expressionResult: ExpressionResult) {
        println(prefix + " " + expressionResult.correspondingTokensText(tokens))
    }

    fun ExpressionResult.tokens(): String {
        return this.correspondingTokensText(this@ExpressionResultsHandlerContext.tokens)
    }
}

fun ExpressionResultsHandlerContext.printExpressionResultO(expressionResult: ExpressionResult, indents: Int = 0) {
    when (expressionResult) {
        is MultiExpressionResult -> {
            println(expressionResult.mainExpressionResult.expression.text() + "   " + expressionResult.mainExpressionResult.range + "   " + expressionResult.tokens())
            if (expressionResult.isNotEmpty()) {
                println(nIndent(3 * (indents + 1)) + "sub expression results of " + expressionResult.mainExpressionResult.expression.text() + " :")
            }
            expressionResult.forEach {
                printExpressionResultO(it, indents + 1)
            }
        }

        is ContentExpressionResult -> {
            println(expressionResult.mainExpressionResult.expression.text() + "   " + expressionResult.mainExpressionResult.range + "   " + expressionResult.tokens())
            println(nIndent(3 * (indents + 1)) + "contents of " + expressionResult.mainExpressionResult.expression.text() + " :")
            printExpressionResultO(expressionResult.content, indents + 1)
        }

        is ContinueExpressionResult -> {
            println(expressionResult.mainExpressionResult.expression.text() + "   " + expressionResult.mainExpressionResult.range + "   " + expressionResult.tokens())
            if (expressionResult.content != null) {
                println(nIndent(3 * (indents + 1)) + "continuing of " + expressionResult.mainExpressionResult.expression.text() + " :")
                printExpressionResultO(expressionResult.content, indents + 1)
            }
        }

        else -> {
            println(nIndent(3 * indents) + expressionResult.expression.text() + "   " + expressionResult.range + "   " + expressionResult.tokens())
        }
    }
}

class NoExpressionResultsError<T>(range: IntRange): Error<T>("expected tokens not found", range)
