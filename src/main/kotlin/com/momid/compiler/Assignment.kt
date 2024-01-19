package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val assignment =
    anyOf(arrayAccess, atomicExp, propertyAccess)["variable"] + spaces + !"=" + spaces + wanting(complexExpression["value"], !"\n") + spaces

fun ExpressionResultsHandlerContext.handleAssignment(currentGeneration: CurrentGeneration): Result<Boolean> {
    with(this.expressionResult) {
        val variable = this["variable"]
        val value = this["value"].continuing {
            return Error("expected expression, found " + it.tokens, it.range)
        }
        val (variableEvaluation, variableOutputType) = continueWithOne(variable, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }
        val (valueEvaluation, valueOutputType) = continueWithOne(value, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }
        if (!typesMatch(variableOutputType, valueOutputType).first) {
            return Error("incompatible types. expected " + variableOutputType + " received " + valueOutputType, value.range)
        }

        currentGeneration.currentScope.generatedSource += assignment(variableEvaluation, valueEvaluation) + "\n"
        return Ok(true)
    }
}
