package com.momid.parser.expression

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
    handle: ExpressionResultsHandlerContext<T>.() -> Result<T>
): Result<T> {
    return ExpressionResultsHandlerContext(expressionFinder, expressionResult, tokens, handle).handle()
}

val ExpressionResult.content: ExpressionResult
    get() {
        if (this is ContentExpressionResult) {
            return this.content
        } else {
            throw(Throwable("expression is not a content expression"))
        }
    }

class ExpressionResultsHandlerContext<T>(
    val expressionFinder: ExpressionFinder,
    val expressionResult: ExpressionResult,
    val tokens: List<Char>,
    val handle: ExpressionResultsHandlerContext<T>.() -> Result<T>
) {
    fun <R> continueWith(expressionResult: ExpressionResult, anotherHandler: ExpressionResultsHandlerContext<R>.() -> Result<R>) {
        expressionFinder.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, anotherHandler).anotherHandler()
        }
    }

    fun <R> continueWith(expressionResult: ExpressionResult) {
        expressionFinder.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, handle).handle()
        }
    }

    fun <R> continueWith(expressionResult: ExpressionResult, vararg registerExpressions: Expression, anotherHandler: ExpressionResultsHandlerContext<R>.() -> Result<R>) {
        ExpressionFinder().apply { registerExpressions(registerExpressions.toList()) }.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, anotherHandler).anotherHandler()
        }
    }

    fun <R> continueWith(expressionResult: ExpressionResult, vararg registerExpressions: Expression) {
        ExpressionFinder().apply { registerExpressions(registerExpressions.toList()) }.start(tokens, expressionResult.range).forEach {
            ExpressionResultsHandlerContext(expressionFinder, it, tokens, handle).handle()
        }
    }

    fun <R> continueWithOne(expressionResult: ExpressionResult, vararg registerExpressions: Expression, anotherHandler: ExpressionResultsHandlerContext<R>.() -> Result<R>): Result<R> {
        ExpressionFinder().apply { registerExpressions(registerExpressions.toList()) }.start(tokens, expressionResult.range).apply {
            if (isNotEmpty()) {
                return ExpressionResultsHandlerContext(expressionFinder, this[0], tokens, anotherHandler).anotherHandler()
            } else {
                return Error("is more than one", IntRange.EMPTY)
            }
        }
    }

    fun continueStraight(
        expressionResult: ExpressionResult,
        anotherHandler: ExpressionResultsHandlerContext<T>.() -> Result<T>
    ): Result<T> {
        return ExpressionResultsHandlerContext(this.expressionFinder, expressionResult, this.tokens, handle).anotherHandler()
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
