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
        val parameters = this["infoParameters"].continuing?.asMulti()?.map {
            val evaluation = continueWithOne(it, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
                return it.to()
            }
            evaluation
        }.orEmpty()
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

            currentGeneration.currentScope.generatedSource += memoryAllocateList(
                cTypeAndVariableName(CReferenceType(Type(cStruct.name)), infoCStructInstanceVariableName),
                cTypeName(Type(cStruct.name)),
                cTypeName(CReferenceType(Type(cStruct.name))),
                3
            ) + "\n"

//            currentGeneration.currentScope.generatedSource += variableDeclaration(
//                cTypeAndVariableName(Type(cStruct.name), infoCStructInstanceVariableName),
//                cStructInitialization(
//                    cStruct.name,
//                    (cStruct.variables.dropLast(1).mapIndexed { index, cStructVariable ->
//                        Pair(cStructVariable.name, parameters[index].first)
//                    } as ArrayList).apply {
//                        this.add(Pair("info_value", valueEvaluation))
//                    }
//                )
//            )

            currentGeneration.currentScope.generatedSource += assignment(
                arrayAccess(infoCStructInstanceVariableName, 0.toString()),
                cStructInitialization(
                    cStruct.name,
                    (cStruct.variables.dropLast(1).mapIndexed { index, cStructVariable ->
                        Pair(cStructVariable.name, parameters[index].first)
                    } as ArrayList).apply {
                        this.add(Pair("info_value", valueEvaluation))
                    }
                )
            ) + "\n"

            currentGeneration.infosInformation.infosInformation[info] = InfoInformation(cStruct, infoCStructInstanceVariableName, 0)
        } else {
            val existingInfo = currentGeneration.infosInformation.infosInformation[info]!!
            currentGeneration.currentScope.generatedSource += assignment(propertyAccess(existingInfo.cListInstanceVariableName, "info_value"), valueEvaluation)
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
