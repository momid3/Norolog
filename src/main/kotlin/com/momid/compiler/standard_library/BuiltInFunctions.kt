package com.momid.compiler.standard_library

import com.momid.compiler.CurrentGeneration
import com.momid.compiler.FunctionCallEvaluating
import com.momid.compiler.arraySet
import com.momid.compiler.cFunctionPointerCall
import com.momid.compiler.output.ClassType
import com.momid.compiler.output.FunctionType
import com.momid.compiler.output.GenericClass
import com.momid.compiler.output.OutputType
import com.momid.parser.expression.ExpressionResultsHandlerContext
import com.momid.parser.expression.Ok
import com.momid.parser.expression.Result

fun ExpressionResultsHandlerContext.handleListSetFunction(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val index = functionCall.parameters[0].cEvaluation
    val item = functionCall.parameters[1].cEvaluation
    val list = (functionCall.receiver!!.outputType as ClassType).outputClass as GenericClass
    val listType = list.typeParameters[0].substitutionType!!
    val receiver = functionCall.receiver!!.cEvaluation
    val listCPointer = receiver + ".list_pointer"
    return Ok(Pair(arraySet(listCPointer, index, item), listType))
}

fun ExpressionResultsHandlerContext.handleLambdaInvokeFunction(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val lambdaReturnType = (functionCall.receiver!!.outputType as FunctionType).outputFunction.returnType
    val receiverLambda = functionCall.receiver!!.cEvaluation
    val cFunctionPointerInvoke = cFunctionPointerCall(receiverLambda, functionCall.parameters.map { it.cEvaluation })
    return Ok(Pair(cFunctionPointerInvoke, lambdaReturnType))
}

fun isLambdaInvokeFunction(functionCall: FunctionCallEvaluating): Boolean {
    return functionCall.receiver != null && functionCall.receiver!!.outputType is FunctionType && functionCall.name.tokens == "invoke"
}
