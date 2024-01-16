package com.momid.compiler

import com.momid.compiler.output.ArrayType
import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*
import com.momid.parser.not

val arrayInitialization =
    !"[" + spaces + wanting(anything["itemsValue"], !",") + spaces + !"," + spaces + number["size"] + !"]"

fun ExpressionResultsHandlerContext.handleArrayInitialization(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val itemsValue = this["itemsValue"]
        val size = this["size"]
        val (evaluation, outputType) = continueWithOne(itemsValue, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }
        return Ok(Pair(arrayInitialization(evaluation, size.tokens.toInt() - 1), ArrayType(outputType, size.tokens.toInt())))
    }
}
