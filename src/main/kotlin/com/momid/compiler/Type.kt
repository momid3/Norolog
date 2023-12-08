package com.momid.compiler

import com.momid.compiler.output.ClassType
import com.momid.compiler.output.GenericClass
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.ReferenceType
import com.momid.parser.expression.*
import com.momid.parser.not

val classType by lazy {
    className["className"] + not(spaces + !"<")
}

val referenceType by lazy {
    !"ref" + space + spaces + anything["actualType"]
}

val functionTypeParameters: CustomExpressionValueic by lazy {
    oneOrZero(
        inline(
            wanting(anything["parameterOutputType"], !",")
                    + some0(one(!"," + spaces + wanting(anything["parameterOutputType"], !",")))
        )
    )
}

val functionType by lazy {
    insideOf('(', ')') {
        functionTypeParameters["functionTypeParameters"]
    } + spaces + !"->" + spaces + anything["functionReturnType"]
}

val genericTypeParameters by lazy {
    inline(
        wanting(anything["typeParameterOutputType"], !",")
                + some0(one(!"," + spaces + wanting(anything["typeParameterOutputType"], !",")))
    )
}

val genericClassType by lazy {
    classType["className"] + spaces + insideOf('<', '>') {
        genericTypeParameters["genericTypes"]
    }
}

val outputTypeO by lazy {
    spaces + anyOf(classType, referenceType, functionType, genericClassType)["outputType"] + spaces
}

fun ExpressionResultsHandlerContext.handleOutputType(currentGeneration: CurrentGeneration): Result<OutputType> {
    this.expressionResult.isOf(outputTypeO) {
        println("is output type")
        println(it.expression::class)
    }
    println("is not output type")
    with(this.expressionResult["outputType"]) {
        println(this::class)
        content.isOf(classType) {
            val className = it["className"].tokens()
            val outputType = ClassType(resolveType(className, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range))
            return Ok(outputType)
        }

        content.isOf(referenceType) {
            val actualType = it["actualType"]
            val actualOutputType = continueWithOne(actualType) { handleOutputType(currentGeneration) }.okOrReport {
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
                val parameterOutputType = continueWithOne(typeParameter["typeParameterOutputType"]) { handleOutputType(currentGeneration) }.okOrReport {
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
