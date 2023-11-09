package com.momid.parser.structure

import com.momid.parser.expression.spaces

fun spaced(expressionTemplates: () -> ExpressionTemplates): ExpressionTemplates {
    val spacedExpressionTemplates = ExpressionTemplates()
    val templates = expressionTemplates()
    for (expressionTemplateIndex in 0..templates.lastIndex - 1) {
        spacedExpressionTemplates.templateItems.add(templates[expressionTemplateIndex])
        spacedExpressionTemplates.templateItems.add(ExpressionTemplate(templateExpression = spaces))
    }
    spacedExpressionTemplates.templateItems.add(templates[templates.lastIndex])
    return spacedExpressionTemplates
}
