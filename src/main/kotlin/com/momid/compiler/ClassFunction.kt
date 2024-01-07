package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val parameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val functionReturnType =
    oneOrZero(spaces + !":" + spaces + outputTypeO["returnType"])

val classFunctionTypeParameters =
    insideOf('<', '>') {
        typeParameters
    }

val classFunction =
    !"fun" + space + oneOrZero(classFunctionTypeParameters["typeParameters"]) + outputTypeO["className"] +
            !"." + className["functionName"] + insideOf('(', ')') {
        oneOrZero(splitBy(parameter, ","))["functionParameters"]
    } + spaces + functionReturnType["functionReturnType"] + spaces + insideOf('{', '}') {
        anything["functionInside"]
    }

fun ExpressionResultsHandlerContext.handleClassFunctionParsing(): Result<ClassFunctionParsing> {
    with(this.expressionResult) {
        val functionName = this["functionName"].parsing
        val className = this["className"].parsing
        val functionParameters = this["functionParameters"].continuing?.continuing?.asMulti()?.map {
            val parameter = it.continuing {
                return Error("expected function parameter, found " + it.tokens, it.range)
            }
            ClassFunctionParameterParsing(parameter["parameterName"].parsing, parameter["parameterType"].parsing)
        }.orEmpty()
        val typeParameters = this["typeParameters"].continuing?.continuing?.asMulti()?.map {
            val typeParameter = it.continuing {
                return Error("expected type parameter, found " + it.tokens, it.range)
            }
            typeParameter.parsing
        }.orEmpty()
        val functionReturnType = this["functionReturnType"].continuing?.get("returnType")?.parsing
        val functionInside = this["functionInside"].parsing
        return Ok(ClassFunctionParsing(functionName, functionParameters, typeParameters, functionReturnType, className, functionInside))
    }
}

fun ExpressionResultsHandlerContext.handleClassFunction(currentGeneration: CurrentGeneration): Result<Boolean> {
    val classFunctionParsing = handleClassFunctionParsing().okOrReport {
        return it.to()
    }

    with(classFunctionParsing) {

    }
}

class ClassFunctionParameterParsing(val name: Parsing, type: Parsing)

class ClassFunctionParsing(
    val name: Parsing,
    val parameters: List<ClassFunctionParameterParsing>,
    val typeParameters: List<Parsing>,
    val returnType: Parsing?,
    val className: Parsing,
    val functionInside: Parsing
)
