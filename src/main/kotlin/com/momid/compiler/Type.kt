package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val classType by lazy {
    not(spaces + !"ref" + space) + className["className"] + not(spaces + anyOf(!"<", !"["))
}

val referenceType by lazy {
    !"ref" + space + anything["actualType"]
}

val arrayType by lazy {
    !"[" + spaces + outputTypeO["actualType"] + spaces + !"," + spaces + number["size"] + spaces + !"]"
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
    with(this.expressionResult["outputType"]) {
        content.isOf(classType) {
            val className = it["className"].tokens()
            val resolvedOutputType = resolveOutputType(className, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range)
            return Ok(resolvedOutputType)
        }

        content.isOf(referenceType) {
            val actualType = it["actualType"]
            val actualOutputType = continueWithOne(actualType, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                if (it is NoExpressionResultsError) {
                    println("expected type, found" + tokens.slice(it.range.first until it.range.last))
                }
                println(it.error)
                return it.to()
            }
            val referenceOutputType = ReferenceType(actualOutputType, "")
            return Ok(referenceOutputType)
        }

        content.isOf(arrayType) {
            val actualType = it["actualType"]
            val actualOutputType = continueStraight(actualType) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }
            val size = it["size"].tokens.toInt()
            val arrayType = ArrayType(actualOutputType, size)
            return Ok(arrayType)
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
                val parameterOutputType = continueWithOne(typeParameter["typeParameterOutputType"], outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
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

        return Error("other types are not usable yet: " + this.tokens(), this.range)
    }
}

fun ExpressionResult.asMulti(): MultiExpressionResult {
    if (this is MultiExpressionResult) {
        return this
    } else {
        throw (Throwable("this expression result is not a multi expression result"))
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "SomeClass".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(classType))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleOutputType(currentGeneration)
        }
    }
}
