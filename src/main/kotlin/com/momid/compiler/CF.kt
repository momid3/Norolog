package com.momid.compiler

import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*

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

val cf by lazy {
    anyOf(className["cfName"] + spaces + insideOf("classInside", '(', ')'), ci)
}

fun ExpressionResultsHandlerContext.handleCF(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    this.expressionResult.isOf(cf) {
        it.content.isOf(ci) {
            return continueWithOne(it, ci) { handleCI(currentGeneration) }
        }
        val cf = it.content
        val cfName = cf["cfName"].tokens()
        if (cfName[0].isUpperCase()) {
            return continueWithOne(cf, ci) { handleCI(currentGeneration) }
        } else {
            return continueWithOne(cf, functionCall) { handleFunctionCall(currentGeneration) }
        }
    }
    return Error("is not cf", this.expressionResult.range)
}
