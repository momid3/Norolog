package com.momid

import com.momid.compiler.*
import com.momid.compiler.output.*
import com.momid.parser.expression.*

val infoParameters =
    splitByNW(complexExpression["infoParameter"], ",")

val info =
    className["infoName"] + insideOf('(', ')') {
        infoParameters["infoParameters"]
    } + spaces + "=" + spaces + complexExpression["value"]

fun ExpressionResultsHandlerContext.handleInfo(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val name = this["infoName"]
        val parameters = this["infoParameters"].asMulti().map {
            val evaluation = continueWithOne(it, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
                return it.to()
            }
            evaluation
        }
        val value = this["value"]
        val (valueEvaluation, valueOutputType) = continueWithOne(value, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            return it.to()
        }

        val info = Info(name.tokens, parameters.map { InfoParameter(it.second) }, valueOutputType)

        if (!currentGeneration.infosInformation.infosInformation.contains(info)) {
            val cStruct = CStruct(
                currentGeneration.createCStructName("info"),
                (parameters.mapIndexed { index, parameter ->
                    CStructVariable("info_parameter_" + index, resolveType(parameter.second, currentGeneration))
                } as ArrayList).apply {
                    this.add(CStructVariable("info_value", resolveType(valueOutputType, currentGeneration)))
                }
            )

            currentGeneration.globalDefinitionsGeneratedSource += cStruct(cStruct.name, cStruct.variables.map {
                cTypeAndVariableName(it.type, it.name)
            })

            val infoCStructInstanceVariableName = createVariableName()

            currentGeneration.generatedSource += variableDeclaration(
                cTypeAndVariableName(Type(cStruct.name), infoCStructInstanceVariableName),
                cStructInitialization(
                    cStruct.name,
                    (cStruct.variables.mapIndexed { index, cStructVariable ->
                        Pair(cStructVariable.name, parameters[index].first)
                    } as ArrayList).apply {
                        this.add(Pair("info_value", valueEvaluation))
                    }
                )
            )

            currentGeneration.infosInformation.infosInformation[info] = Pair(cStruct, infoCStructInstanceVariableName)
        } else {
            val existingInfo = currentGeneration.infosInformation.infosInformation[info]!!
            currentGeneration.generatedSource += assignment(propertyAccess(existingInfo.second, "info_value"), valueEvaluation)
        }

        return Ok(Pair("", norType))
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "someFunction(anotherFunction(someParameter0, someParameter1, someParameter3), parameter1, parameter3)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(info))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println(this.tokens.joinToString(""))
            handleInfo(currentGeneration)
        }
    }
}
