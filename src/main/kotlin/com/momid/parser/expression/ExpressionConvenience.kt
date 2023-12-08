package com.momid.parser.expression

import com.momid.parser.match
import com.momid.parser.printExpressionResult

infix fun Expression.withName(name: String): Expression {
    val expression = this.clone()
    expression.name = name
    expression.isValueic = true
    return expression
}

operator fun Expression.get(name: String): Expression {
    return this.withName(name)
}

operator fun MultiExpressionResult.get(name: String): ExpressionResult {
    return this.find { it.expression.name == name } ?: throw(Throwable("there is no sub expressionresult with this name: " + name))
}

operator fun ExpressionResult.get(name: String): ExpressionResult {
    when (this) {
        is MultiExpressionResult -> return this[name]
        else -> throw(Throwable("this expressionresult kind does not have sub expressionresults"))
    }
}

inline fun <T : Expression> ExpressionResult.isOf(expression: T, then: (ExpressionResult) -> Unit) {
    if (this.expression == expression) {
        then(this)
    }
}

inline fun ExpressionResult.forEach(block: (ExpressionResult) -> Unit) {
    when (this) {
        is MultiExpressionResult -> this.forEach { block(it) }
        else -> throw(Throwable("this expressionresult kind does not have sub expressionresults"))
    }
}

fun <T : Expression> ExpressionResult.isOfForEach(expression: T, block: (ExpressionResult) -> Unit) {
    this.isOf(expression) {
        it.forEach {
            block(it)
        }
    }
}

fun not(expression: Expression): NotExpression {
    return NotExpression(expression)
}

@JvmName("not1")
operator fun Expression.not(): NotExpression {
    return not(this)
}

fun MultiExpressionResult.getForName(name: String): IntRange? {
    return this.find { it.expression.name == name }?.range
}

operator fun Expression.plus(expression: Expression): MultiExpression {
    if (this is MultiExpression) {
        if (this.name == null && expression.name == null) {
            return this + expression
        } else {
            return MultiExpression(arrayListOf(this, expression))
        }
    } else {
        return MultiExpression(arrayListOf(this, expression))
    }
}

operator fun Expression.plus(any: Any): MultiExpression {
    if (this is MultiExpression) {
        return this + any.asExpression()
    } else {
        return MultiExpression(arrayListOf(this, any.asExpression()))
    }
}

operator fun MultiExpression.plus(expression: Expression): MultiExpression {
    this.expressions.add(expression)
    return this
}

operator fun Expression.times(value: Int): RecurringExpression {
    return RecurringExpression(this, value)
}

//operator fun Condition

fun some(expression: Expression): RecurringSomeExpression {
    return RecurringSomeExpression(expression)
}

fun some0(expression: Expression): RecurringSome0Expression {
    return RecurringSome0Expression(expression)
}

fun anyOf(vararg expressions: Expression): EachOfExpression {
    return EachOfExpression(expressions.asList())
}

fun anyOf(vararg token: Char): EachOfTokensExpression {
    return EachOfTokensExpression(token.asList())
}

fun condition(condition: (Char) -> Boolean): ConditionExpression {
    return ConditionExpression(condition)
}

//operator fun Any.plus(any: Any): MultiExpression {
//    return this.asExpression() + any.asExpression()
//}

fun exact(expression: String): ExactExpression {
    return ExactExpression(expression)
}

//operator fun String.not(): ExactExpression {
//    return ExactExpression(this)
//}

inline fun <reified T> Any.isOfType(): Boolean {
    return this is T
}

inline fun <reified T> Any.castTo(): T {
    if (this is T) {
        return this
    } else {
        throw (Throwable("types are incompatible"))
    }
}

fun Any.asExpression(): Expression {
    when (this) {
        is String -> return ExactExpression(this)
        else -> throw (Throwable("this type is not convertable to expression"))
    }
}

fun Expression.clone(): Expression {
    return when (this) {
        is ExactExpression -> ExactExpression(this.value).apply { this.id = this@clone.id }
        is ConditionExpression -> ConditionExpression(this.condition).apply { this.id = this@clone.id }
        is MultiExpression -> MultiExpression(this.expressions).apply { this.id = this@clone.id }
        is RecurringExpression -> RecurringExpression(this.expression, this.numberOfRecurring).apply { this.id = this@clone.id }
        is RecurringSomeExpression -> RecurringSomeExpression(this.expression).apply { this.id = this@clone.id }
        is RecurringSome0Expression -> RecurringSome0Expression(this.expression).apply { this.id = this@clone.id }
        is EachOfExpression -> EachOfExpression(this).apply { this.id = this@clone.id }
        is EachOfTokensExpression -> EachOfTokensExpression(this).apply { this.id = this@clone.id }
        is CustomExpression -> CustomExpression(this.condition).apply { this.id = this@clone.id }
        is CustomExpressionValueic -> CustomExpressionValueic(this.condition).apply { this.id = this@clone.id }
        else -> throw (Throwable("unknown expression kind"))
    }
}

fun main() {

    val text = "hello! my friend. how are you today ?"
    val side = condition { it != 'a' }

    val expression = side["side before"] + "friend" + side["side after"]
    val matches = match(expression, text.toList())
    matches.forEach {
        if (it is MultiExpressionResult) {
            println(it["side after"]?.range)
        }
    }
    matches.forEach {
        printExpressionResult(it)
    }
}