package com.momid.compiler

import com.momid.compiler.output.ArrayType
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.outputIntType
import com.momid.parser.expression.*

val arrayAccess =
    anyOf(atomicExp, propertyAccess, simpleExpressionInParentheses)["array"] + insideOf('[', ']') {
        wanting(complexExpression)["index"]
    }

fun ExpressionResultsHandlerContext.handleArrayAccessParsing(): Result<ArrayAccessParsing> {
    with(this.expressionResult) {
        val array = this["array"]
        val index = this["index"].continuing?.continuing {
            return Error("expected array access index, found " + it.tokens, it.range)
        } ?: return Error("expected array access index", this["index"].range)
        return Ok(ArrayAccessParsing(array.parsing, index.parsing))
    }
}

fun ExpressionResultsHandlerContext.handleArrayAccess(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    println("here")
    val arrayAccessParsing = handleArrayAccessParsing().okOrReport {
        println("here")
        return it.to()
    }

    with(arrayAccessParsing) {
        val (evaluation, outputType) = continueWithOne(this.array.expressionResult, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            println("here")
            return it.to()
        }
        if (outputType !is ArrayType) {
            return Error("access operator is only applicable to arrays", this.array.range)
        }

        val (indexEvaluation, indexOutputType) = continueWithOne(this.index.expressionResult, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            println("there")
            return it.to()
        }

        if (indexOutputType != outputIntType) {
            return Error("access index should be an int", this.index.range)
        }

        val output = arrayAccess(evaluation, indexEvaluation)
        return Ok(Pair(output, outputType.itemsType))
    }
}

class ArrayAccessParsing(val array: Parsing, val index: Parsing)
