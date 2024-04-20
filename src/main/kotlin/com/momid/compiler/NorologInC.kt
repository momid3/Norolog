package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val norologVariableInC =
    !"#" + className["variableName"]

fun handlePossibleNorologVariableInC(currentGeneration: CurrentGeneration, cExpressionText: String): Result<String> {

    val textPieces = ArrayList<Pair<String, Boolean>>()

    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(norologVariableInC))
    val finds = finder.startDiscover(cExpressionText.toList())
    if (finds.isEmpty()) {
        return Ok(cExpressionText)
    }
    var currentIndex = 0
    finds.forEachIndexed { index, expressionResult ->
        handleExpressionResult(finder, expressionResult, cExpressionText.toList()) {
            with(this.expressionResult) {
                val variableName = this["variableName"]
                val (evaluation, outputType) = continueWithOne(
                    variableName,
                    complexExpression
                ) { handleComplexExpression(currentGeneration) }.okOrReport {
                    return@handleExpressionResult it.to()
                }
                textPieces.add(Pair(cExpressionText.slice(currentIndex until this.range.first), false))
                textPieces.add(Pair(evaluation, true))
                if (index == finds.lastIndex) {
                    textPieces.add(Pair(cExpressionText.slice(this.range.last until cExpressionText.length), false))
                }

                currentIndex = variableName.range.last

                return@handleExpressionResult Ok(true)
            }
        }
    }

    return Ok(textPieces.joinToString("") { it.first })
}
