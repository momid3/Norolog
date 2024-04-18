package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*
import com.momid.parser.not

val cExpression = !"#" + insideOf('(', ')') {
    cExpressionParameters
}["cExpressionParameters"]

val cExpressionParameters = CustomExpressionValueic() { tokens, startIndex, endIndex, thisExpression ->
    val expressionResults = ArrayList<ExpressionResult>()
    var commaIndex = endIndex
    while (true) {
        commaIndex -= 1
        if (tokens[commaIndex] == ',') {
            break
        }
        if (commaIndex <= startIndex) {
            return@CustomExpressionValueic null
        }
    }
    val cExpression = ExpressionResult(Expression(), startIndex..commaIndex, commaIndex)
    val outputType = ExpressionResult(Expression(), (commaIndex + 1)..endIndex, endIndex)
    expressionResults.add(cExpression)
    expressionResults.add(outputType)
    return@CustomExpressionValueic MultiExpressionResult(ExpressionResult(thisExpression, startIndex..endIndex, endIndex), expressionResults)
}

fun ExpressionResultsHandlerContext.handleCExpression(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val parameters = this["cExpressionParameters"].continuing {
            return Error("expecting c expression found " + it.tokens, it.range)
        }.asMulti()
        val cExpression = parameters[0].tokens
        val outputType = continueWithOne(parameters[1], one(spaces + outputTypeO["outputType"] + spaces)) { handleOutputType(currentGeneration) }.okOrReport {
            return it.to()
        }

        return Ok(Pair(cExpression, outputType))
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "#(3, Int)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(cExpression))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleCExpression(currentGeneration)
        }
    }
}
