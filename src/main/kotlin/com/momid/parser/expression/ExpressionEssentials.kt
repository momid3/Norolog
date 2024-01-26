package com.momid.parser.expression

import com.momid.parser.not

val spaces = some0(condition { it.isWhitespace() }).apply { this.isValueic = false }

val insideParentheses = CustomExpression() { tokens, startIndex, endIndex ->
    var numberOfLefts = 1
    for (tokenIndex in startIndex..endIndex - 1) {
        if (tokens[tokenIndex] == '(') {
            numberOfLefts += 1
        }
        if (tokens[tokenIndex] == ')') {
            numberOfLefts -= 1
        }
        if (numberOfLefts == 0) {
            return@CustomExpression tokenIndex
        }
    }
    return@CustomExpression -1
}

fun inline(multiExpression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        val inlinedExpressionResults = ArrayList<ExpressionResult>()
        val expressionResult = evaluateExpressionValueic(multiExpression, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        if (expressionResult !is MultiExpressionResult) {
            throw(Throwable("expression " + multiExpression::class + "does not evaluate to a MultiExpressionResult"))
        }
        expressionResult.forEach {
            if (it is MultiExpressionResult) {
                inlinedExpressionResults.addAll(it.expressionResults)
            } else {
                inlinedExpressionResults.add(it)
            }
        }
        expressionResult.expressionResults.clear()
        expressionResult.expressionResults.addAll(inlinedExpressionResults)
        return@CustomExpressionValueic ContentExpressionResult(ExpressionResult(multiExpression, expressionResult.range), expressionResult)
    }
}

fun spaced(expressions: () -> MultiExpression): MultiExpression {
    val spacedExpressions = MultiExpression(arrayListOf(spaces))
    val expressions = expressions()
    for (expressionIndex in 0..expressions.lastIndex - 1) {
        spacedExpressions.expressions.add(expressions[expressionIndex])
        spacedExpressions.expressions.add(spaces)
    }
    spacedExpressions.expressions.add(expressions[expressions.lastIndex])
    spacedExpressions.expressions.add(spaces)
    return spacedExpressions
}

fun insideOfParentheses(parenthesesStart: Char, parenthesesEnd: Char): CustomExpression {
    return CustomExpression() { tokens, startIndex, endIndex ->
        var numberOfLefts = 1
        for (tokenIndex in startIndex..endIndex - 1) {
            if (tokens[tokenIndex] == parenthesesStart) {
                numberOfLefts += 1
            }
            if (tokens[tokenIndex] == parenthesesEnd) {
                numberOfLefts -= 1
            }
            if (numberOfLefts == 0) {
                return@CustomExpression tokenIndex
            }
        }
        return@CustomExpression -1
    }
}

fun insideOf(expression: Expression, parenthesesStart: Char, parenthesesEnd: Char): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        if (tokens[startIndex] != parenthesesStart) {
            return@CustomExpressionValueic null
        }
        var numberOfLefts = 1
        for (tokenIndex in startIndex + 1 until endIndex) {
            if (tokens[tokenIndex] == parenthesesStart) {
                numberOfLefts += 1
            }
            if (tokens[tokenIndex] == parenthesesEnd) {
                numberOfLefts -= 1
            }
            if (numberOfLefts == 0) {
                return@CustomExpressionValueic ContentExpressionResult(ExpressionResult(expression, startIndex .. tokenIndex + 1), ExpressionResult(expression, startIndex + 1 .. (tokenIndex + 1) - 1))
            }
        }
        return@CustomExpressionValueic null
    }
}

fun insideOf(name: String, parenthesesStart: Char, parenthesesEnd: Char): CustomExpressionValueic {
    val expression = (!"")[name]
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        if (tokens[startIndex] != parenthesesStart) {
            return@CustomExpressionValueic null
        }
        var numberOfLefts = 1
        for (tokenIndex in startIndex + 1 until endIndex) {
            if (tokens[tokenIndex] == parenthesesStart) {
                numberOfLefts += 1
            }
            if (tokens[tokenIndex] == parenthesesEnd) {
                numberOfLefts -= 1
            }
            if (numberOfLefts == 0) {
                return@CustomExpressionValueic ContentExpressionResult(ExpressionResult(expression, startIndex .. tokenIndex + 1), ExpressionResult(expression, startIndex + 1 .. (tokenIndex + 1) - 1))
            }
        }
        return@CustomExpressionValueic null
    }
}

fun insideOf(parenthesesStart: Char, parenthesesEnd: Char, expression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        if (startIndex >= endIndex) {
            return@CustomExpressionValueic null
        }
        if (tokens[startIndex] != parenthesesStart) {
            return@CustomExpressionValueic null
        }
        var numberOfLefts = 1
        for (tokenIndex in startIndex + 1 until endIndex) {
            if (tokens[tokenIndex] == parenthesesStart) {
                numberOfLefts += 1
            }
            if (tokens[tokenIndex] == parenthesesEnd) {
                numberOfLefts -= 1
            }
            if (numberOfLefts == 0) {
                val evaluation = evaluateExpressionValueic(expression, startIndex + 1, tokens, (tokenIndex + 1) - 1)
                return@CustomExpressionValueic ContinueExpressionResult(ExpressionResult(expression, startIndex .. tokenIndex + 1), evaluation)
            }
        }
        return@CustomExpressionValueic null
    }
}

fun insideOf(parenthesesStart: Char, parenthesesEnd: Char, expression: () -> Expression): CustomExpressionValueic {
    return CustomExpressionValueic() { tokens, startIndex, endIndex ->
        if (startIndex >= endIndex) {
            return@CustomExpressionValueic null
        }
        if (tokens[startIndex] != parenthesesStart) {
            return@CustomExpressionValueic null
        }
        var numberOfLefts = 1
        for (tokenIndex in startIndex + 1 until endIndex) {
            if (tokens[tokenIndex] == parenthesesStart) {
                numberOfLefts += 1
            }
            if (tokens[tokenIndex] == parenthesesEnd) {
                numberOfLefts -= 1
            }
            if (numberOfLefts == 0) {
                val evaluation = evaluateExpressionValueic(expression(), startIndex + 1, tokens, (tokenIndex + 1) - 1)
                return@CustomExpressionValueic ContinueExpressionResult(ExpressionResult(expression(), startIndex .. tokenIndex + 1), evaluation)
            }
        }
        return@CustomExpressionValueic null
    }
}

fun wanting(expression: Expression, until: Expression): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        var nextTokenIndex = startIndex
        while (true) {
            val evaluation = evaluateExpressionValueic(until, nextTokenIndex, tokens, endIndex)
            if (evaluation == null) {
                nextTokenIndex += 1
            } else {
                break
            }
            if (nextTokenIndex >= endIndex) {
                break
            }
        }
        val evaluation = evaluateExpressionValueic(expression, startIndex, tokens, nextTokenIndex)
        return@CustomExpressionValueic ContinueExpressionResult(ExpressionResult(expression, startIndex..nextTokenIndex), evaluation)
    }
}

fun wanting(expression: Expression): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        val evaluation = evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        return@CustomExpressionValueic ContinueExpressionResult(ExpressionResult(expression, startIndex..endIndex), evaluation)
    }
}

fun insideParentheses(expression: Expression): Expression {
    return combineExpressions(insideParentheses, expression)
}

fun matchesFully(expression: Expression, tokenSlice: List<Char>): Boolean {
    return evaluateExpression(expression, 0, tokenSlice) == tokenSlice.size
}

fun combineExpressions(expression: Expression, otherExpression: Expression): Expression {
    return CustomExpression() { tokens, startIndex, endIndex ->
        val expressionNextIndex = evaluateExpression(expression, startIndex, tokens, endIndex)
        if (expressionNextIndex == -1) {
            return@CustomExpression -1
        }
        if (matchesFully(otherExpression, tokens.slice(startIndex until expressionNextIndex))) {
            return@CustomExpression expressionNextIndex
        } else {
            return@CustomExpression -1
        }
    }
}

fun and(vararg expressions: Expression): CustomExpression {
    return CustomExpression() { tokens, startIndex, endIndex ->
        var maxNextIndex = 0
        for (expression in expressions) {
            val nextIndex = evaluateExpression(expression, startIndex, tokens, endIndex)
            if (nextIndex == -1) {
                return@CustomExpression -1
            } else {
                if (nextIndex > maxNextIndex) {
                    maxNextIndex = nextIndex
                }
            }
        }
        return@CustomExpression maxNextIndex
    }
}

fun Expression.andAlso(vararg otherExpressions: Expression): CustomExpressionValueic {
    return CustomExpressionValueic { tokens, startIndex, endIndex ->
        val evaluation = evaluateExpressionValueic(this, startIndex, tokens, endIndex) ?: return@CustomExpressionValueic null
        for (expression in otherExpressions) {
            evaluateExpressionValueic(expression, startIndex, tokens, endIndex)?.nextTokenIndex ?: return@CustomExpressionValueic null
        }
        return@CustomExpressionValueic evaluation
    }
}
