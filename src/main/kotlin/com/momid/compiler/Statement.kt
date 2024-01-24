package com.momid.compiler

import com.momid.compiler.output.ReferenceType
import com.momid.compiler.output.Type
import com.momid.compiler.output.VariableInformation
import com.momid.compiler.output.createVariableName
import com.momid.parser.expression.*
import com.momid.parser.not

val space by lazy {
    some(condition { it.isWhitespace() })
}

fun space(): RecurringSomeExpression {
    return some(condition { it.isWhitespace() })
}

val variableDeclaration by lazy {
    !"val" + space + variableNameO["variableName"] + spaces + "=" + spaces + complexExpression["assignmentExpression"] + ";"
}

val functionCallStatement =
    spaces + functionCall["function"] + ";"


fun ExpressionResultsHandlerContext.handleVariableDeclaration(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(variableDeclaration) {
        println("is assignment statement")
        val output: String
        val evaluation = continueStraight(it["assignmentExpression"]) {
            handleComplexExpression (currentGeneration)
        }
        if (evaluation is Error<*>) {
            println("assignment has error")
            currentGeneration.errors.add(evaluation)
            println(evaluation.error)
            return Error(evaluation.error, evaluation.range)
        }
        if (evaluation is Ok) {
            val expressionEvaluation = evaluation.ok.first
            val expressionType = evaluation.ok.second

//            if (expressionType is ReferenceType) {
//                println("no variable assignment is created for reference type")
//                return Ok("\n")
//            }

            val variableName = if (expressionType is ReferenceType) {
                "pointer_" + createVariableName()
            } else {
                createVariableName()
            }
            currentGeneration.currentScope.variables.add(
                VariableInformation(
                    variableName,
                    Type.Int,
                    expressionEvaluation,
                    it["variableName"].tokens(),
                    expressionType
                )
            )

            val cTypeAndVariableName = cTypeAndVariableName(expressionType, variableName, currentGeneration)
            output = cTypeAndVariableName + " = " + evaluation.ok.first + ";" + "\n"
            currentGeneration.currentScope.generatedSource += output
            println("generated assignment: " + output)
            return Ok(output)
        }
    }
    return Error("is not assignment", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleFunctionCallStatement(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(functionCallStatement) {
        println("is function call")
//        val evaluation = continueWithOne(it["function"], function) { handleFunction(currentGeneration) }
        val evaluation = continueStraight(it["function"]) { handleFunctionCall(currentGeneration) }

        if (evaluation is Ok) {
            currentGeneration.currentScope.generatedSource += evaluation.ok.first + ";" + "\n"
            return Ok("")
        }
        if (evaluation is Error) {
            currentGeneration.errors.add(evaluation)
            println(evaluation.error)
            return evaluation.to()
        }
    }
    return Error("is not a function call", this.expressionResult.range)
}

fun main() {
    val text = "print(3 + 7 + 37);".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(functionCallStatement))

    val currentGeneration = CurrentGeneration()
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleFunctionCallStatement(currentGeneration)
        }
    }

    println(currentGeneration.generatedSource)
}
