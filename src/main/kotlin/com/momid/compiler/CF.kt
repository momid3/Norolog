package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*
import com.momid.parser.not

/***
 * CF is for "class initialization or function call"
 */
class CF()

//val cfVariableExp =
//    spaces + complexExpression["parameter"] + spaces

//val cfVariable =
//    ignoreParentheses(condition { it != ',' && it != ')' })

//val cfVariables =
//    spaces + inline(cfVariable["classVariable"] + inline(some0(spaces + !"," + spaces + cfVariable["classVariable"])) + spaces) + spaces

//val variableName = variableName0()

val cf =
    className["cfName"] + spaces + insideOf("classInside", '(', ')')

val space0 =
    some(condition { it.isWhitespace() })

fun cf(): MultiExpression {
    return !"class" + space + variableNameO["cfName"] + spaces + insideOf("classInside", '(', ')')
}

fun ExpressionResultsHandlerContext.handleCF(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    this.expressionResult.isOf(cf) {
        val cfName = it["cfName"].tokens()
        if (cfName[0].isUpperCase()) {
            return continueWithOne(it, ci) { handleCI(currentGeneration) }
        } else {
            return continueWithOne(it, functionCall) { handleFunctionCall(currentGeneration) }
        }
    }
    return Error("is not cf", this.expressionResult.range)
}
