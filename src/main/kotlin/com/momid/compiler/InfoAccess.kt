package com.momid.compiler

import com.momid.compiler.output.InfoParameter
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.createVariableName
import com.momid.parser.expression.*
import com.momid.parser.not

val infoAccess =
    !"#" + className["infoName"] + insideOf('(', ')') {
        infoParameters["infoParameters"]
    }

fun ExpressionResultsHandlerContext.handleInfoAccess(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    with(this.expressionResult) {
        val name = this["infoName"]
        val parameters = this["infoParameters"].continuing?.asMulti()?.map {
            val evaluation = continueWithOne(it, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
                return it.to()
            }
            evaluation
        }.orEmpty()

        val info = currentGeneration.infosInformation.infosInformation.keys.find {
            it.name == name.tokens && it.parameters.forEveryIndexed { index, parameter ->
                parameter == InfoParameter(parameters[index].second)
            } && it.declarationPackage == ""
        }

        if (info == null) {
            return Error("no such info exists", this.range)
        }

        val existingInfo = currentGeneration.infosInformation.infosInformation[info]!!
        val cListInstanceVariable = existingInfo.cListInstanceVariableName
        val listCurrentIndexVariableName = existingInfo.listCurrentIndexVariableName
        val listSizeVariableName = existingInfo.listSizeVariableName

        val infoValueVariableName = createVariableName("info_value")

        val foundVariableName = createVariableName("found")

        currentGeneration.currentScope.generatedSource +=
            """
                    |bool $foundVariableName = false;
                    |${variableDeclaration(cTypeAndVariableName(info.valueType, infoValueVariableName, currentGeneration))}
                    |
                    |for (int index = 0; index < $listSizeVariableName; index += 1) {
                    |    if (${compareInfoWithCStruct("$cListInstanceVariable[index]", parameters, existingInfo.cStruct)}) {
                    |        ${assignment(infoValueVariableName, "$cListInstanceVariable[index].info_value")}
                    |        $foundVariableName = true;
                    |    }
                    |}
                    |
                    |if (!$foundVariableName) {
                    |    printf("info is unknown\n");
                    |} else {
                    |    printf("info found\n");
                    |}
                    |
               """.trimMargin()

        return Ok(Pair(infoValueVariableName, info.valueType))
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
