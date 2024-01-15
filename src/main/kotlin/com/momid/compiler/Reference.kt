package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.compiler.output.ReferenceType
import com.momid.compiler.output.createVariableName
import com.momid.parser.expression.Error
import com.momid.parser.expression.ExpressionResultsHandlerContext
import com.momid.parser.expression.Ok
import com.momid.parser.expression.Result

fun ExpressionResultsHandlerContext.handleReferenceFunction(functionCall: FunctionCallEvaluation, currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        var output = ""
        if (functionCall.parameters.size != 1) {
            return Error("ref function should have only one parameter", this.range)
        }
        val (parameterEvaluation, parameterType) = functionCall.parameters[0]
        val cPointerName = createVariableName()
        val temporaryEvaluationVariableName = "temporary_" + createVariableName()
        val parameterCTypeAndName = cTypeAndVariableName(parameterType, temporaryEvaluationVariableName, currentGeneration)
        val parameterCTypeName = cTypeName(parameterType, currentGeneration)
        val temporaryEvaluationVariable = variableDeclaration(parameterCTypeAndName, parameterEvaluation) + "\n"
        currentGeneration.currentScope.generatedSource += "\n"
        currentGeneration.currentScope.generatedSource += temporaryEvaluationVariable
        currentGeneration.currentScope.generatedSource += memoryAllocate(parameterCTypeAndName, parameterCTypeName) + "\n"
        currentGeneration.currentScope.generatedSource += memoryCopy(cPointerName, temporaryEvaluationVariableName, parameterCTypeName) + "\n"
        currentGeneration.currentScope.generatedSource += "\n"
        return Ok(Pair(cPointerName, ReferenceType(parameterType, cPointerName)))
    }
}
