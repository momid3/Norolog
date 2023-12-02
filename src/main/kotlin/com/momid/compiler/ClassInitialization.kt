package com.momid.compiler

import com.momid.compiler.output.Class
import com.momid.compiler.output.ClassType
import com.momid.compiler.output.OutputType
import com.momid.parser.expression.*
import com.momid.parser.not

/***
 * CI is for "class initialization"
 */
class CI()

val className =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })

val ciVariableExp by lazy {
    spaces + complexExpression["parameter"] + spaces
}

val ciVariable =
    ignoreParentheses(condition { it != ',' && it != ')' })

val ciVariables =
    spaces + inline(ciVariable["classVariable"] + inline(some0(spaces + !"," + spaces + ciVariable["classVariable"])) + spaces) + spaces

val classInitialization by lazy {
    className["className"] + spaces + insideOf("ciInside", '(', ')')
}

/***
 * @return the output of this class initialization that can be used or inserted to the output
 */
fun ExpressionResultsHandlerContext.handleClassInitialization(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    this.expressionResult.isOf(classInitialization) {
        println("class initialization: " + it.tokens())
        var output = ""
        val className = it["className"].tokens()
        val resolvedClass = resolveType(className, currentGeneration) ?: return Error("could not resolve class: " + className, it["className"].range)
        val parameters = continueWithOne(it["ciInside"].content, ciVariables) { handleClassInitializationParameters(currentGeneration) }
        parameters.handle({
            return Error(it.error, it.range)
        }, {
            if (!validateCIParameterTypes(resolvedClass, it.map { it.second })) {
                return Error("class parameters are not of expected type", this.expressionResult.range)
            }

            output += cStructInitialization(resolveClass(resolvedClass, currentGeneration).name, resolvedClass.variables.mapIndexed { index, classVariable ->
                Pair(classVariable.name, it[index].first)
            })
            return Ok(Pair(output, ClassType(resolvedClass)))
        })
    }
    println("is not class initialization")
    return Error("is not class initialization", this.expressionResult.range)
}

/***
 * evaluates the expression of each parameter of the class initialization and return a pair of, the value that can be
 * inserted to the output and the type of the value of the expression, for each of the parameters
 */
fun ExpressionResultsHandlerContext.handleClassInitializationParameters(currentGeneration: CurrentGeneration): Result<List<Pair<String, OutputType>>> {
    this.expressionResult.isOf(ciVariables) {
        val evaluation = ArrayList<Pair<String, OutputType>>()
        it.forEach {
            it.forEach {
                val parameterExpression = continueWithOne(it, ciVariableExp) { handleClassInitializationParameter(currentGeneration) }
                parameterExpression.handle({
                    return Error(it.error, it.range)
                }, {
                    val parameterEvaluation = continueStraight(it) { handleComplexExpression(currentGeneration) }
                    parameterEvaluation.handle({
                        return Error(it.error, it.range)
                    }, {
                        evaluation.add(it)
                    })
                })
            }
        }
        return Ok(evaluation)
    }
    return Error("is not ci variables", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleClassInitializationParameter(currentGeneration: CurrentGeneration): Result<ExpressionResult> {
    this.expressionResult.isOf(ciVariableExp) {
        return Ok(it["parameter"])
    }
    return Error("the parameter is not an expression", this.expressionResult.range)
}

fun validateCIParameterTypes(ciClass: Class, ciParameters: List<OutputType>): Boolean {
    ciClass.variables.forEachIndexed { index, classVariable ->
        if (classVariable.type != ciParameters[index]) {
            println("type mismatch: expected: " + classVariable.type + " got: " + ciParameters[index])
            return false
        }
    }
    return true
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "SomeClass(3, 3, 3)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(classInitialization))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleClassInitialization(currentGeneration)
        }
    }
}
