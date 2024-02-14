package com.momid.compiler.standard_library

import com.momid.compiler.*
import com.momid.compiler.output.*
import com.momid.parser.expression.*

fun ExpressionResultsHandlerContext.handleGraphicsInit(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    return Ok(Pair(initGraphics(), outputIntType))
}

fun ExpressionResultsHandlerContext.handleCreateWindow(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val width = functionCall.parameters[0]
    val height = functionCall.parameters[1]

    val output = createWindow(width.cEvaluation, height.cEvaluation)

    return Ok(Pair(output, ReferenceType(ClassType(window), "")))
}

fun ExpressionResultsHandlerContext.handleSleep(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val seconds = functionCall.parameters[0].cEvaluation
    val output = sleep(seconds)
    return Ok(Pair(output, norType))
}

fun ExpressionResultsHandlerContext.handleCreateRenderer(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val window = functionCall.parameters[0]
    val output = createRenderer(window.cEvaluation)
    return Ok(Pair(output, ReferenceType(ClassType(renderer), "")))
}

fun ExpressionResultsHandlerContext.handleDrawLine(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val parameters = functionCall.parameters
    val paint = parameters[0].parsing!!.tokens
    val renderer = parameters[1].cEvaluation
    val colorA = continueComplexExpression(paint + ".color.a", currentGeneration) {
        return it.to()
    }.first
    val colorR = continueComplexExpression(paint + ".color.r", currentGeneration) {
        return it.to()
    }.first
    val colorG = continueComplexExpression(paint + ".color.g", currentGeneration) {
        return it.to()
    }.first
    val colorB = continueComplexExpression(paint + ".color.b", currentGeneration) {
        return it.to()
    }.first

    currentGeneration.currentScope.generatedSource += setRendererColor(renderer, colorA, colorR, colorG, colorB) + ";\n"

    val x0 = parameters[2].cEvaluation
    val y0 = parameters[3].cEvaluation
    val x1 = parameters[4].cEvaluation
    val y1 = parameters[5].cEvaluation

    val output = drawLine(renderer, x0, y0, x1, y1)
    return Ok(Pair(output, norType))
}

fun ExpressionResultsHandlerContext.handleUpdate(
    functionCall: FunctionCallEvaluating,
    currentGeneration: CurrentGeneration
): Result<Pair<String, OutputType>> {
    val renderer = functionCall.parameters[0].cEvaluation
    val output = update(renderer)
    return Ok(Pair(output, norType))
}

fun <T> continueGiven(
    tokens: String,
    vararg expressions: Expression,
    anotherHandler: ExpressionResultsHandlerContext.() -> Result<T>
): Result<T> {
    val finder = ExpressionFinder()
    finder.registerExpressions(expressions.toList())
    finder.start(tokens.toList()).apply {
        if (isNotEmpty()) {
            return ExpressionResultsHandlerContext(finder, this[0], tokens.toList(), anotherHandler).anotherHandler()
        } else {
            return NoExpressionResultsError(IntRange.EMPTY)
        }
    }
}

inline fun continueComplexExpression(tokens: String, currentGeneration: CurrentGeneration, report: (Error<*>) -> Unit): Pair<String, OutputType> {
    return continueGiven(tokens, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport(report)
}
