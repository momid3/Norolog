package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val space =
    some(condition { it.isWhitespace() })

val assignment =
    !"val" + space + variableName["variableName"] + spaces + "=" + spaces + complexExpression["assignmentExpression"] + ";"

val functionCall =
    spaces + function["function"] + ";"


fun ExpressionResultsHandlerContext<String>.handleAssignment(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(assignment) {
        println("is assignment statement")
        val output: String
        val evaluation = continueStraight(it["assignmentExpression"]) {
            handleComplexExpression (currentGeneration)
        }
        if (evaluation is Error<*>) {
            println("assignment has error")
            currentGeneration.errors.add(evaluation)
            println(evaluation.error)
            return evaluation
        }
        if (evaluation is Ok<String>) {
            val variableName = createVariableName()
            currentGeneration.currentScope.variables.add(
                VariableInformation(
                    it["variableName"].correspondingTokensText(tokens),
                    Type("int"),
                    evaluation.ok.toInt(),
                    variableName,
                    OutputType.Int
                )
            )
            output = "int " + variableName + " = " + evaluation.ok + ";" + "\n"
            currentGeneration.generatedSource += output
            println("generated assignment: " + output)
            return Ok(output)
        }
    }
    return Error("is not assignment", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext<String>.handleFunctionCall(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(functionCall) {
        println("is function call")
//        val evaluation = continueWithOne(it["function"], function) { handleFunction(currentGeneration) }
        val evaluation = continueStraight(it["function"]) { handleFunction(currentGeneration) }

        if (evaluation is Ok<String>) {
            return Ok("")
        }
        if (evaluation is Error<*>) {
            currentGeneration.errors.add(evaluation)
            println(evaluation.error)
            return evaluation
        }
    }
    return Error("is not a function call", this.expressionResult.range)
}

fun main() {
    val text = "print(3 + 7 + 37);".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(functionCall))

    val currentGeneration = CurrentGeneration()
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleFunctionCall(currentGeneration)
        }
    }

    println(currentGeneration.generatedSource)
}
