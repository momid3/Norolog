package com.momid.parser.structure

import com.momid.parser.expression.Expression

open class ExpressionTemplate(val templateName: String? = null, val templateExpression: Expression? = null): Template()

class ExpressionTemplates(val templateItems: ArrayList<ExpressionTemplate> = ArrayList()): Template(), List<ExpressionTemplate> by templateItems

open class Template()

fun Template.getExpressionTemplates(): ExpressionTemplates {
    when (this) {
        is Expression -> {
            return ExpressionTemplates(arrayListOf(ExpressionTemplate(templateExpression = this)))
        }
        is ExpressionTemplate -> {
            return ExpressionTemplates(arrayListOf(this))
        }
        is ExpressionTemplates -> {
            return this
        }
        else -> throw(Throwable("unknown template kind"))
    }
}

fun Template.toExpression(): Expression? {
    if (this is Expression) {
        return this
    } else {
        return null
    }
}

fun Template.toExpressionTemplateIfExpression(): Template {
    if (this is Expression) {
        return ExpressionTemplate(templateExpression = this)
    } else {
        return this
    }
}

fun ExpressionTemplate.isTemplate(): Boolean {
    return this.templateName != null
}

fun ExpressionTemplate.isExpression(): Boolean {
    return this.templateExpression != null
}

