package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val infoParameters =
    splitByNW(complexExpression["infoParameter"], ",")

val info =
    !"#" + className["infoName"] + insideOf('(', ')') {
        infoParameters["infoParameters"]
    } + spaces + "=" + spaces + complexExpression["value"] + spaces + !";"

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

            val infoListInstanceVariable = createVariableName("info_list")

            currentGeneration.currentScope.generatedSource += memoryAllocateList(
                cTypeAndVariableName(CReferenceType(Type(cStruct.name)), infoListInstanceVariable),
                cTypeName(Type(cStruct.name)),
                cTypeName(CReferenceType(Type(cStruct.name))),
                3
            ) + "\n"

            currentGeneration.currentScope.generatedSource += assignment(
                arrayAccess(infoListInstanceVariable, 0.toString()),
                cStructInitialization(
                    cStruct.name,
                    (cStruct.variables.dropLast(1).mapIndexed { index, cStructVariable ->
                        Pair(cStructVariable.name, parameters[index].first)
                    } as ArrayList).apply {
                        this.add(Pair("info_value", valueEvaluation))
                    }
                )
            ) + "\n"

            val listCurrentIndexVariableName = createVariableName("info_list_current_index")
            currentGeneration.currentScope.generatedSource += variableDeclaration(cTypeAndVariableName(Type.Int, listCurrentIndexVariableName), "0") + "\n"

            val listSizeVariableName = createVariableName("info_list_size")
            currentGeneration.currentScope.generatedSource += variableDeclaration(cTypeAndVariableName(Type.Int, listSizeVariableName), "3") + "\n"

            currentGeneration.infosInformation.infosInformation[info] = InfoInformation(cStruct, infoListInstanceVariable, listCurrentIndexVariableName, listSizeVariableName)
        } else {
            val existingInfo = currentGeneration.infosInformation.infosInformation[info]!!
            val cListInstanceVariable = existingInfo.cListInstanceVariableName
            val listCurrentIndexVariableName = existingInfo.listCurrentIndexVariableName
            val listSizeVariableName = existingInfo.listSizeVariableName

            val foundVariableName = createVariableName("found")

            currentGeneration.currentScope.generatedSource +=
                """
                    |bool $foundVariableName = false;
                    |
                    |for (int index = 0; index < $listSizeVariableName; index += 1) {
                    |    if (${compareInfoWithCStruct("$cListInstanceVariable[index]", parameters, existingInfo.cStruct)}) {
                    |        ${assignment(propertyAccess("$cListInstanceVariable[index]", "info_value"), valueEvaluation)}
                    |        $foundVariableName = true;
                    |    }
                    |}
                    |
                    |if (!$foundVariableName) {
                    |    $listCurrentIndexVariableName += 1;
                    |    $cListInstanceVariable[$listCurrentIndexVariableName] = ${
                            cStructInitialization(
                                existingInfo.cStruct.name, 
                                (existingInfo.cStruct.variables.dropLast(1).mapIndexed { index, cStructVariable -> 
                                    Pair(cStructVariable.name, parameters[index].first) 
                                } as ArrayList).apply { 
                                    this.add(Pair("info_value", valueEvaluation)) 
                                }
                            )
                        };
                    |    printf("not found\n");
                    |} else {
                    |    printf("found\n");
                    |}
                    |
               """.trimMargin()
        }

        return Ok(Pair("", norType))
    }
}

fun compareInfoWithCStruct(cInfoListVariable: String, infoParameters: List<Pair<String, OutputType>>, cStruct: CStruct): String {
    var output = ""
    for (index in cStruct.variables.indices.toList().dropLast(1)) {
        output += "$cInfoListVariable.${cStruct.variables[index].name} == ${infoParameters[index].first}"
        if (index != cStruct.variables.lastIndex - 1) {
            output += " && "
        }
    }
    return output
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
