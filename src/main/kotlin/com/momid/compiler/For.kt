package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val insideOfFor =
    Expression()

val forStatement =
    !"for" + spaces + "(" + spaces + variableNameO["variableName"] + spaces + "in" + spaces + complexExpression["rangeStart"] +
            spaces + "until" + spaces + complexExpression["rangeEnd"] + spaces + ")" + spaces +
            (insideOf("forInside", '{', '}'))["forInside"]

fun ExpressionResultsHandlerContext.handleForLoop(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(forStatement) {
        println("is for loop: " + it.tokens())
        var output = ""
        val rangeStart = continueStraight(it["rangeStart"]) { handleComplexExpression(currentGeneration) }
        val rangeEnd = continueStraight(it["rangeEnd"]) { handleComplexExpression(currentGeneration) }
        val indexName = createVariableName()
        val indexVariable = VariableInformation(it["variableName"].tokens(), Type.Int, 0, indexName, OutputType(outputInt))
        val scope = Scope()
        scope.variables.add(indexVariable)
        println("inside of for loop: " + it["forInside"].content.tokens())
        val insideForLoop = continueStraight(it["forInside"].content) { handleCodeBlock(currentGeneration, scope) }

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
    println("is not for loop")
    return Error("is not for loop", this.expressionResult.range)
}
