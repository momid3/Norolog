package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val insideOfFor =
    Expression()

val forStatement =
    !"for" + spaces + "(" + spaces + variableName["variableName"] + spaces + "in" + spaces + complexExpression["rangeStart"] +
            spaces + "until" + spaces + complexExpression["rangeEnd"] + spaces + ")" + spaces +
            inlineContent(insideOf(insideOfFor, '{', '}')["forInside"])

fun ExpressionResultsHandlerContext.handleForLoop(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(forStatement) {
        var output = ""
        val rangeStart = continueStraight(it["rangeStart"]) { handleComplexExpression(currentGeneration) }
        val rangeEnd = continueStraight(it["rangeEnd"]) { handleComplexExpression(currentGeneration) }
        val indexName = createVariableName()
        val indexVariable = VariableInformation(indexName, Type.Int, 0, it["variableName"].tokens(), OutputType("Int"))
        val scope = Scope()
        scope.variables.add(indexVariable)
        val insideForLoop = continueStraight(it["forInside"]) { handleCodeBlock(currentGeneration, scope) }

        if (rangeStart is Ok) {
            if (rangeEnd is Ok) {
                if (insideForLoop is Ok) {
                    output += forLoop(indexName, rangeStart.ok.first, rangeEnd.ok.first, insideForLoop.ok)
                    currentGeneration.currentScope.generatedSource += output
                    return Ok("")
                }
            }
        }

        return Error("some error happened when evaluating for loop", it.range)
    }
    return Error("is not for loop", this.expressionResult.range)
}
