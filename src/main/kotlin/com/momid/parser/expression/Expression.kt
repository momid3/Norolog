package com.momid.parser.expression

import com.momid.parser.structure.Template

var currentExpressionId = 0

fun createExpressionId(): Int {
    currentExpressionId += 1
    return currentExpressionId
}

public open class Expression(var name: String? = null, var isValueic: Boolean = true, var id: Int = createExpressionId()): Template() {
    override fun equals(other: Any?): Boolean {
        return other is Expression && other.id == this.id
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + isValueic.hashCode()
        return result
    }
}

class ExactExpression(val value: String) : Expression(isValueic = false) {

}

class ConditionExpression(val condition: (Char) -> Boolean): Expression()

open class MultiExpression(val expressions: ArrayList<Expression>): Expression(), List<Expression> by expressions

class RecurringExpression(val expression: Expression, val numberOfRecurring: Int): Expression()

class RecurringSomeExpression(val expression: Expression): Expression()

class RecurringSome0Expression(val expression: Expression): Expression()

class EachOfExpression(private val expressions: List<Expression>): Expression(), List<Expression> by expressions

class EachOfTokensExpression(private val tokens: List<Char>): Expression(), List<Char> by tokens

class NotExpression(val expression: Expression): Expression()

class CustomExpression(val condition: (tokens: List<Char>, startIndex: Int, endIndex: Int) -> Int): Expression()

class CustomExpressionValueic(val condition: (tokens: List<Char>, startIndex: Int, endIndex: Int) -> ExpressionResult?): Expression()

interface Condition {

    public fun invoke(token: Char): Boolean
}

interface EvaluateExpression {

    fun evaluate(startIndex: Int, tokens: List<Char>): Int
}

fun evaluateExpression(expression: Expression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    if (startIndex >= tokens.size) {
        if (expression is RecurringSome0Expression) {
            return startIndex
        } else {
            return -1
        }
    }
    when (expression) {
        is ExactExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is ConditionExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is MultiExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is RecurringExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is RecurringSomeExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is RecurringSome0Expression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is EachOfExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is EachOfTokensExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is NotExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        is CustomExpression -> return evaluateExpression(expression, startIndex, tokens, endIndex)
        else -> throw(Throwable("unknown expression kind"))
    }
}

fun evaluateExpressionValueic(expression: Expression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): ExpressionResult? {
    if (startIndex >= tokens.size) {
        if (expression is RecurringSome0Expression) {
            return ExpressionResult(expression, startIndex..startIndex)
        } else {
            return null
        }
    }
    when (expression) {
        is MultiExpression -> return evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        is EachOfExpression -> return evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        is RecurringSomeExpression -> return evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        is RecurringSome0Expression -> return evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        is CustomExpressionValueic -> return evaluateExpressionValueic(expression, startIndex, tokens, endIndex)
        else -> {
            val tokensEndIndex = evaluateExpression(expression, startIndex, tokens, endIndex)
            if (tokensEndIndex != -1) {
                return ExpressionResult(expression, startIndex .. tokensEndIndex)
            } else {
                return null
            }
        }
    }
}

fun evaluateExpression(exactExpression: ExactExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    var exactExpressionIndex = 0
    var tokensEndIndex = startIndex
    for (index in startIndex until endIndex) {
        tokensEndIndex += 1
        if (tokens[index] == exactExpression.value[exactExpressionIndex]) {
            exactExpressionIndex += 1
            if (exactExpressionIndex == exactExpression.value.length) {
                return tokensEndIndex
            }
        } else {
            return -1
        }
    }
    return -1
}

fun evaluateExpression(conditionExpression: ConditionExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    if (conditionExpression.condition(tokens[startIndex])) {
        return startIndex + 1
    } else return -1
}

fun evaluateExpression(multiExpression: MultiExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    var multiExpressionIndex = 0
    var tokensEndIndex = startIndex
    while (true) {
        val nextIndex = evaluateExpression(multiExpression[multiExpressionIndex], tokensEndIndex, tokens, endIndex)
        if (nextIndex == -1) {
            return -1
        } else {
            tokensEndIndex = nextIndex
            multiExpressionIndex += 1
            if (multiExpressionIndex == multiExpression.size) {
                return tokensEndIndex
            }
            if (tokensEndIndex > endIndex) {
                break
            }
        }
    }
    return -1
}

fun evaluateExpressionValueic(multiExpression: MultiExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): MultiExpressionResult? {
    val expressionResults = ArrayList<ExpressionResult>()
    var multiExpressionIndex = 0
    var tokensEndIndex = startIndex
    while (true) {
        val evaluationResult = evaluateExpressionValueic(multiExpression[multiExpressionIndex], tokensEndIndex, tokens, endIndex)
        if (evaluationResult == null) {
            return null
        } else {

            val nextIndex = evaluationResult.range.last
            val expression = multiExpression[multiExpressionIndex]
            if (expression.isValueic) {
                expressionResults.add(evaluationResult)
            }

            tokensEndIndex = nextIndex
            multiExpressionIndex += 1
            if (multiExpressionIndex == multiExpression.size) {
                if (expressionResults.isEmpty()) {
                    throw(Throwable("multiExpression subs should not be empty"))
//                    return ExpressionResult(multiExpression, startIndex .. endIndex)
                } else {
                    return MultiExpressionResult(ExpressionResult(multiExpression, startIndex .. tokensEndIndex), expressionResults)
                }
            }
            if (tokensEndIndex > endIndex) {
                break
            }
        }
    }
    return null
}

fun evaluateExpression(recurringExpression: RecurringExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    val recurringList = MutableList(recurringExpression.numberOfRecurring) {
        recurringExpression.expression
    }
    return evaluateExpression(MultiExpression(recurringList as ArrayList<Expression>), startIndex, tokens, endIndex)
}

fun evaluateExpression(eachOfExpression: EachOfExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    eachOfExpression.forEach {
        val tokensEndIndex = evaluateExpression(it, startIndex, tokens, endIndex)
        if (tokensEndIndex != -1) {
            return tokensEndIndex
        }
    }
    return -1
}

fun evaluateExpressionValueic(eachOfExpression: EachOfExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): ContentExpressionResult? {
    eachOfExpression.forEach {
        val expressionResult = evaluateExpressionValueic(it, startIndex, tokens, endIndex)
        if (expressionResult != null) {
            val tokensEndIndex = expressionResult.range.last
            if (tokensEndIndex != -1) {
                return ContentExpressionResult(ExpressionResult(eachOfExpression, expressionResult.range), expressionResult)
            }
        }
    }
    return null
}

fun evaluateExpression(eachOfTokensExpression: EachOfTokensExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    eachOfTokensExpression.forEach {
        if (tokens[startIndex] == it) {
            return startIndex + 1
        }
    }
    return -1
}

fun evaluateExpression(recurringSomeExpression: RecurringSomeExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    var numberOfRecurring = 0
    var tokensEndIndex = startIndex
    while (true) {
        val nextIndex = evaluateExpression(recurringSomeExpression.expression, tokensEndIndex, tokens, endIndex)
        if (nextIndex == -1) {
            break
        } else {
            tokensEndIndex = nextIndex
            if (tokensEndIndex <= endIndex) {
                numberOfRecurring += 1
            } else {
                break
            }
        }
    }
    if (numberOfRecurring > 0) {
        return tokensEndIndex
    } else {
        return -1
    }
}

fun evaluateExpressionValueic(recurringSomeExpression: RecurringSomeExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): ExpressionResult? {
    val expressionResults = ArrayList<ExpressionResult>()
    var numberOfRecurring = 0
    var tokensEndIndex = startIndex
    while (true) {
        val expressionResult = evaluateExpressionValueic(recurringSomeExpression.expression, tokensEndIndex, tokens, endIndex) ?: break
        tokensEndIndex = expressionResult.range.last
        if (tokensEndIndex <= endIndex) {
            numberOfRecurring += 1
            expressionResults.add(expressionResult)
        } else {
            break
        }
    }
    if (numberOfRecurring > 0) {
        return MultiExpressionResult(ExpressionResult(recurringSomeExpression, startIndex..tokensEndIndex), expressionResults)
    } else {
        return null
    }
}

fun evaluateExpression(recurringSome0Expression: RecurringSome0Expression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    var numberOfRecurring = 0
    var tokensEndIndex = startIndex
    while (true) {
        val nextIndex = evaluateExpression(recurringSome0Expression.expression, tokensEndIndex, tokens, endIndex)
        if (nextIndex == -1) {
            break
        } else {
            tokensEndIndex = nextIndex
            if (tokensEndIndex <= endIndex) {
                numberOfRecurring += 1
            } else {
                break
            }
        }
    }
    return tokensEndIndex
}

fun evaluateExpressionValueic(recurringSome0Expression: RecurringSome0Expression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): ExpressionResult {
    val expressionResults = ArrayList<ExpressionResult>()
    var numberOfRecurring = 0
    var tokensEndIndex = startIndex
    while (true) {
        val expressionResult = evaluateExpressionValueic(recurringSome0Expression.expression, tokensEndIndex, tokens, endIndex) ?: break
        tokensEndIndex = expressionResult.range.last
        if (tokensEndIndex <= endIndex) {
            numberOfRecurring += 1
            expressionResults.add(expressionResult)
        } else {
            break
        }
    }
    return MultiExpressionResult(ExpressionResult(recurringSome0Expression, startIndex..tokensEndIndex), expressionResults)
}

fun evaluateExpression(notExpression: NotExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    val tokensEndIndex = evaluateExpression(notExpression.expression, startIndex, tokens, endIndex)
    if (tokensEndIndex == -1) {
        return startIndex
    } else {
        return -1
    }
}

fun evaluateExpression(customExpression: CustomExpression, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): Int {
    val tokensEndIndex = customExpression.condition(tokens, startIndex, endIndex)
    return tokensEndIndex
}

fun evaluateExpressionValueic(customExpression: CustomExpressionValueic, startIndex: Int, tokens: List<Char>, endIndex: Int = tokens.size): ExpressionResult? {
    val expressionResult = customExpression.condition(tokens, startIndex, endIndex)
    return expressionResult
}


fun main() {
    val text = "hello ! what a beautiful day. how are you ?"
    var endIndex = evaluateExpression(ExactExpression("hello"), 0, text.toList())
    endIndex = evaluateExpression(ConditionExpression { it != 'h' }, 0, text.toList())
    endIndex = evaluateExpression(MultiExpression(arrayListOf(ExactExpression("hello"), ConditionExpression { it != 'a' }, ExactExpression("!"))), 0, text.toList())
    println("end index is: " + endIndex)
}
