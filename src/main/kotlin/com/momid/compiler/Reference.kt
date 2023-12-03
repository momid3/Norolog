package com.momid.compiler

import com.momid.compiler.output.*
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
        val temporaryEvaluationVariable = variableDeclaration(temporaryEvaluationVariableName, cTypeName(parameterType, currentGeneration), parameterEvaluation) + "\n"
        currentGeneration.currentScope.generatedSource += "\n"
        currentGeneration.currentScope.generatedSource += temporaryEvaluationVariable
        currentGeneration.currentScope.generatedSource += memoryAllocation(cPointerName, cTypeName(parameterType, currentGeneration)) + "\n"
        currentGeneration.currentScope.generatedSource += memoryCopy(cPointerName, temporaryEvaluationVariableName, cTypeName(parameterType, currentGeneration)) + "\n"
        currentGeneration.currentScope.generatedSource += "\n"
        return Ok(Pair(cPointerName, ReferenceType(parameterType, cPointerName)))
    }
}

/***
 * @return the output of this c type that can be used or inserted to the output
 */
fun cTypeName(type: Type): String {
    return when (type) {
        Type.Int -> type.name
        Type.Boolean -> type.name
        is CReferenceType -> cTypeName(type.actualType) + "*"
        else -> "struct " + type.name
    }
}

fun cTypeName(outputType: OutputType, currentGeneration: CurrentGeneration): String {
    val cType = resolveType(outputType, currentGeneration)
    return cTypeName(cType)
}
