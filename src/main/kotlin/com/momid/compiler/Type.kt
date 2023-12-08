package com.momid.compiler

import com.momid.compiler.output.ClassType
import com.momid.compiler.output.GenericClass
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.ReferenceType
import com.momid.parser.expression.*
import com.momid.parser.not

val classType =
    className["className"] + not(spaces + !"<")

val referenceType: MultiExpression by lazy {
    !"ref" + space + spaces + outputType["actualType"]
}

val functionTypeParameters: CustomExpressionValueic by lazy {
    oneOrZero(
        inline(
            wanting(outputType["parameterOutputType"], !",")
                    + some0(one(!"," + spaces + wanting(outputType["parameterOutputType"], !",")))
        )
    )
}

val functionType: MultiExpression by lazy {
    insideOf('(', ')') {
        functionTypeParameters["functionTypeParameters"]
    } + spaces + !"->" + spaces + outputType["functionReturnType"]
}

val genericTypeParameters by lazy {
    inline(
        wanting(outputType["typeParameterOutputType"], !",")
                + some0(one(!"," + spaces + wanting(outputType["typeParameterOutputType"], !",")))
    )
}

val genericClassType: MultiExpression by lazy {
    classType["className"] + spaces + insideOf('<', '>') {
        genericTypeParameters["genericTypes"]
    }
}

val outputType by lazy {
    one(spaces + anyOf(classType, referenceType, functionType, genericClassType)["outputType"] + spaces)
}

fun ExpressionResultsHandlerContext.handleOutputType(currentGeneration: CurrentGeneration): Result<OutputType> {
    with(this.expressionResult) {
        content.isOf(classType) {
            val className = it["className"].tokens()
            val outputType = ClassType(resolveType(className, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range))
            return Ok(outputType)
        }

        content.isOf(referenceType) {
            val actualType = it["actualType"]
            val actualOutputType = continueStraight(actualType) { handleOutputType(currentGeneration) }.okOrReport {
                println(it.error)
                return it.to()
            }
            val referenceOutputType = ReferenceType(actualOutputType, "")
            return Ok(referenceOutputType)
        }

        content.isOf(genericClassType) {
            val className = it["className"]["className"]
            val classNameText = it["className"]["className"].tokens()
            val outputType = ClassType(resolveType(classNameText, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range))
            val typeParametersOutputType = it["genericTypes"].asMulti().map {
                val typeParameter = it.continuing {
                    println("expected type Parameter, found: " + it.tokens())
                    return Error("expected type parameter, found: " + it.tokens(), it.range)
                }
                val parameterOutputType = continueStraight(typeParameter["typeParameterOutputType"]) { handleOutputType(currentGeneration) }.okOrReport {
                    println(it.error)
                    return it.to()
                }
                parameterOutputType
            }
            val outputClass = outputType.outputClass
            if (outputClass is GenericClass) {
                outputClass.typeParameters.forEachIndexed { index, genericTypeParameter ->
                    genericTypeParameter.substitutionType = typeParametersOutputType[index]
                }
                return Ok(outputType)
            } else {
                return Error("this class does not have any type variables", className.range)
            }
        }

        return Error("other types are not usable yet", this.range)
    }
}

fun ExpressionResult.asMulti(): MultiExpressionResult {
    if (this is MultiExpressionResult) {
        return this
    } else {
        throw (Throwable("this expression result is not a multi expression result"))
    }
}
