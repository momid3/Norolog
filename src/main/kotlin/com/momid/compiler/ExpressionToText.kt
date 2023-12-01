package com.momid.compiler

import com.momid.parser.expression.*

fun Expression.text(): String {
    return when (this) {
        is ExactExpression -> this.text()
        is MultiExpression -> this.text()
        is RecurringExpression -> this.text()
        is RecurringSomeExpression -> this.text()
        is RecurringSome0Expression -> this.text()
        is EachOfExpression -> this.text()
        is EachOfTokensExpression -> this.text()
        is ConditionExpression -> this.text()
        is NotExpression -> this.text()
        is CustomExpression -> this.text()
        is CustomExpressionValueic -> this.text()
        else -> throw (Throwable("unknown expression kind"))
    }
}

fun ExactExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return this.value + name
}

fun MultiExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "(" + this.expressions.joinToString(" + ") {
        it.text()
    } + ")" + name
}

fun RecurringExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "(" + this.expression.text() + ")" + "* " + this.numberOfRecurring + name
}

fun RecurringSomeExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "some(" + this.expression.text() + ")" + name
}

fun RecurringSome0Expression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "some0(" + this.expression.text() + ")" + name
}

fun EachOfExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "anyOf(" + this.joinToString(", ") {
        it.text()
    } + ")" + name
}

fun EachOfTokensExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "anyOf(" + this.joinToString(", ") {
        it.toString()
    } + ")" + name
}

fun ConditionExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "condition(" + ")" + name
}

fun NotExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "not(" + this.expression.text() + ")" + name
}

fun CustomExpression.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "customExpression(" + ")" + name
}

fun CustomExpressionValueic.text(): String {
    val name = if (this.name != null) {
        "[" + this.name + "]"
    } else {
        ""
    }
    return "some(" + ")" + name
}
