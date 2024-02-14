package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val propertyAccessStatement =
    propertyAccess["propertyAccess"] + spaces + !";"

fun ExpressionResultsHandlerContext.handlePropertyAccessStatement(currentGeneration: CurrentGeneration): Result<Boolean> {
    println("here")
    with(this.expressionResult) {
        val propertyAccess = this["propertyAccess"]
        val (evaluation, outputType) = continueStraight(propertyAccess) { handlePropertyAccess(currentGeneration) }.okOrReport {
            return it.to()
        }

        currentGeneration.currentScope.generatedSource += evaluation + ";" + "\n"
        return Ok(true)
    }
}
