package com.momid.compiler

import com.momid.compiler.output.ArrayType
import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*

val arrayInitialization by lazy {
    insideOf('[', ']') {
        arrayInitializationInside
    }
}

fun ExpressionResultsHandlerContext.handleArrayInitialization(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val inside = this.continuing {
            return Error("expecting arrayInitialization found " + it.tokens, it.range)
        }
        val itemsValue = inside["itemsValue"]
        val size = inside["size"]
        val (evaluation, outputType) = continueWithOne(itemsValue, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }
        return Ok(Pair(arrayInitialization(evaluation, size.tokens.toInt() - 1), ArrayType(outputType, size.tokens.toInt())))
    }
}

val arrayInitializationInside = CustomExpressionValueic() { tokens, startIndex, endIndex, thisExpression ->
    val itemsValue = evaluateExpressionValueic(complexExpression["itemsValue"], startIndex, tokens, endIndex) ?:
    return@CustomExpressionValueic null
    if (tokens[itemsValue.nextTokenIndex] != ',') {
        return@CustomExpressionValueic null
    }
    val size = evaluateExpressionValueic(one(spaces + number["size"] + spaces), itemsValue.nextTokenIndex + 1, tokens, endIndex) ?:
    return@CustomExpressionValueic null
    val expressionResults = arrayListOf(itemsValue, size)

    return@CustomExpressionValueic MultiExpressionResult(ExpressionResult(thisExpression, startIndex..endIndex, endIndex), expressionResults)
}
