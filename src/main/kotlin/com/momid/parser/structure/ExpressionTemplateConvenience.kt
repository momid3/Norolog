package com.momid.parser.structure

import com.momid.parser.expression.ExactExpression
import com.momid.parser.expression.Expression
import com.momid.parser.expression.asExpression
import kotlin.reflect.KProperty

operator fun ExpressionTemplate.plus(expressionTemplate: ExpressionTemplate): ExpressionTemplates {
    return ExpressionTemplates(arrayListOf(this, expressionTemplate))
}

operator fun ExpressionTemplates.plus(expressionTemplate: ExpressionTemplate): ExpressionTemplates {
    this.templateItems.add(expressionTemplate)
    return this
}

operator fun String.invoke(): ExpressionTemplate {
    return ExpressionTemplate(this)
}

operator fun String.plus(expressionTemplate: ExpressionTemplate): ExpressionTemplates {
    return this.asExpression() + expressionTemplate
}

operator fun ExpressionTemplate.plus(expression: String): ExpressionTemplates {
    return this + expression.asExpression()
}

operator fun String.not(): ExpressionTemplate {
    return ExpressionTemplate(templateExpression = ExactExpression(this))
}

operator fun ExpressionTemplates.plus(expression: String): ExpressionTemplates {
    return this + ExpressionTemplate(templateExpression = expression.asExpression())
}

operator fun ExpressionTemplate.plus(expression: Expression): ExpressionTemplates {
    return this + ExpressionTemplate(templateExpression = expression)
}

operator fun ExpressionTemplates.plus(expression: Expression): ExpressionTemplates {
    return this + ExpressionTemplate(templateExpression = expression)
}

operator fun Expression.plus(expressionTemplate: ExpressionTemplate): ExpressionTemplates {
    return ExpressionTemplate(templateExpression = this) + expressionTemplate
}

operator fun KProperty<*>.plus(expressionTemplate: ExpressionTemplate): ExpressionTemplates {
    return ExpressionTemplate(this.name) + expressionTemplate
}

operator fun ExpressionTemplate.plus(expressionTemplate: KProperty<*>): ExpressionTemplates {
    return this + ExpressionTemplate(expressionTemplate.name)
}

operator fun ExpressionTemplates.plus(expressionTemplate: KProperty<*>): ExpressionTemplates {
    this.templateItems.add(ExpressionTemplate(expressionTemplate.name))
    return this
}

operator fun KProperty<*>.not(): ExpressionTemplate {
    return ExpressionTemplate(this.name)
}

//operator fun Expression.plus(expression: Expression): ExpressionTemplates {
//    return ExpressionTemplate(templateExpression = expression) + ExpressionTemplate(templateExpression = expression)
//}