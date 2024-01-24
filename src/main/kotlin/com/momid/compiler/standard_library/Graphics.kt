package com.momid.compiler.standard_library

import com.momid.compiler.CurrentGeneration
import com.momid.compiler.FunctionCallEvaluating
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.outputIntType
import com.momid.parser.expression.ExpressionResultsHandlerContext
import com.momid.parser.expression.Ok
import com.momid.parser.expression.Result

fun ExpressionResultsHandlerContext.handleGraphicsInit(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    return Ok(Pair(initGraphics(), outputIntType))
}
