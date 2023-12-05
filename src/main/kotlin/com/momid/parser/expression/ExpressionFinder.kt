package com.momid.parser.expression

class ExpressionFinder {

    var registeredExpressions = ArrayList<Expression>()

    fun registerExpressions(expressions: List<Expression>) {
        registeredExpressions.addAll(expressions)
    }

    fun start(tokens: List<Char>, findingRange: IntRange? = null): List<ExpressionResult> {
        val foundExpressions = ArrayList<ExpressionResult>()
        var currentTokenIndex = findingRange?.start ?: 0
        val maxTokenIndex = findingRange?.last ?: tokens.size
        while (true) { whi@
            for (expression in registeredExpressions) {
                if (currentTokenIndex >= maxTokenIndex) {
                    break@whi
                }
                val expressionResult = evaluateExpressionValueic(expression, currentTokenIndex, tokens, maxTokenIndex) ?: continue
                currentTokenIndex = expressionResult.nextTokenIndex
                foundExpressions.add(expressionResult)
                continue@whi
            }
            break
        }
        return foundExpressions
    }
}
