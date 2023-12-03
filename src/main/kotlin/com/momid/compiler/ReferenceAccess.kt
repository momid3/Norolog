package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.compiler.output.ReferenceType
import com.momid.parser.expression.Error
import com.momid.parser.expression.ExpressionResultsHandlerContext
import com.momid.parser.expression.Ok
import com.momid.parser.expression.Result

/***
 * handles reference access via value accessor. gives the actual value that the reference points to.
 * @param cExpression the evaluated output of the expression.
 * @param expressionType the output type of the value of the expression on which the 'value' access is being made.
 * @return the first element is the output that can be used or inserted to the output and the last element is
 * the output type of the value of the accessed value.
 */
fun ExpressionResultsHandlerContext.handleReferenceAccess(
    cExpression: String,
    expressionType: OutputType,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        if (expressionType is ReferenceType) {
            return Ok(Pair(pointerDereference(cExpression), expressionType.actualType))
        } else {
            return Error("value accessor cannot be applied to a type which is not reference type", this.range)
        }
    }
}
