package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val cCodeBlock =
    !"###" + wanting(anything["cCodeBlock"], !"###") + !"###"

fun ExpressionResultsHandlerContext.handleCCodeBlock(currentGeneration: CurrentGeneration): Result<Boolean> {
    with(this.expressionResult) {
        val cCodeBlock = this["cCodeBlock"]
        val cCodeBlockText = handlePossibleNorologVariableInC(currentGeneration, cCodeBlock.tokens).okOrReport {
            return it.to()
        }
        currentGeneration.currentScope.generatedSource += cCodeBlockText + "\n"
        return Ok(true)
    }
}
