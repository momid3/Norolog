package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.compiler.terminal.blue
import com.momid.parser.expression.*
import com.momid.parser.not

val classType by lazy {
    not(spaces + anyOf(!"ref", !"[") + space) + className["className"] + not(spaces + anyOf(!"<", !"["))
}

val referenceType: MultiExpression by lazy {
    !"ref" + space + outputTypeOOO["actualType"]
}

val arrayType: MultiExpression by lazy {
    !"[" + spaces + wanting(outputTypeOOO["actualType"], !",") + !"," + spaces + number["size"] + spaces + !"]"
}

val functionTypeParameters: CustomExpressionValueic by lazy {
    oneOrZero(splitByNW(one(spaces + outputTypeOOO["parameterOutputType"] + spaces), ","))
}

val functionType by lazy {
    insideOf('(', ')') {
        functionTypeParameters
    }["functionTypeParameters"] + spaces + !"->" + spaces + outputTypeOOO["functionReturnType"]
}

val genericTypeParameters: CustomExpressionValueic by lazy {
    splitByNW(one(spaces + outputTypeOOO["typeParameterOutputType"] + spaces), ",")
}

val genericClassType by lazy {
    className["className"] + spaces + insideOf('<', '>') {
        genericTypeParameters
    }["genericTypes"]
}

val outputTypeO by lazy {
    outputTypeOOO
}


fun ExpressionResultsHandlerContext.handleOutputType(currentGeneration: CurrentGeneration): Result<OutputType> {
    with(this.expressionResult) {
        content.isOf(classType) {
            val className = it["className"].tokens()
            val resolvedOutputType = resolveOutputType(className, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range)
            return Ok(resolvedOutputType)
        }

        content.isOf(referenceType) {
            val actualType = it["actualType"]
            val actualOutputType = continueWithOne(actualType, outputTypeOOO) { handleOutputType(currentGeneration) }.okOrReport {
                if (it is NoExpressionResultsError) {
                    println("expected type, found" + tokens.slice(it.range.first until it.range.last))
                }
                println(it.error)
                return it.to()
            }
            val referenceOutputType = ReferenceType(actualOutputType, "")
            return Ok(referenceOutputType)
        }

        content.isOf(functionType) {
            val parameters = it["functionTypeParameters"].continuing?.continuing?.asMulti()?.map {
                val parameterType = continueWithOne(it, outputTypeOOO) { handleOutputType(currentGeneration) }.okOrReport {
                    return it.to()
                }
                parameterType
            }.orEmpty()
            val returnType = continueWithOne(it["functionReturnType"], outputTypeOOO) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }
            val function = Function("", parameters.map { FunctionParameter("", it) }, returnType, IntRange.EMPTY)
            return Ok(FunctionType(function, null))
        }

        content.isOf(arrayType) {
            println("arrayType")
            val actualType = it["actualType"]
            val actualOutputType = continueWithOne(actualType, outputTypeOOO) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }
            val size = it["size"].tokens.toInt()
            val arrayType = ArrayType(actualOutputType, size)
            return Ok(arrayType)
        }

        content.isOf(genericClassType) {
            println(blue("generic class type " + it.tokens))
            val className = it["className"]
            val classNameText = it["className"].tokens()

            val outputType = ClassType(resolveType(classNameText, currentGeneration) ?:
            return Error("unresolved class: " + className, it["className"].range))

            val typeParametersOutputType = it["genericTypes"].continuing?.asMulti()?.map {
                val typeParameter = it
                val parameterOutputType = continueWithOne(typeParameter, outputTypeOOO) { handleOutputType(currentGeneration) }.okOrReport {
                    println(it.error)
                    return it.to()
                }
                parameterOutputType
            } ?: return Error("expected type parameters", it["genericTypes"].range)
            val outputClass = outputType.outputClass
            if (outputClass is GenericClass) {
                outputClass.typeParameters.forEachIndexed { index, genericTypeParameter ->
                    if (typeParametersOutputType[index] !is TypeParameterType) {
                        println("is not type parameter " + this.tokens)
                        genericTypeParameter.substitutionType = typeParametersOutputType[index]
                    } else {
                        println("is type parameter " + this.tokens)
                        outputClass.typeParameters[index].name = (typeParametersOutputType[index] as TypeParameterType).genericTypeParameter.name
                    }
                }
                outputClass.unsubstituted = false
                if (!isUnsubstitutedGenericClassType(outputClass)) {
                    println(blue("is not unsubstituted"))
                    createGenericClassIfNotExists(currentGeneration, outputClass)
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

fun isUnsubstitutedType(outputType: OutputType): Boolean {
    return (
            outputType is TypeParameterType && (outputType.genericTypeParameter.substitutionType == null ||
                    (outputType.genericTypeParameter.substitutionType != null && isUnsubstitutedType(outputType.genericTypeParameter.substitutionType!!)))
            )
}

fun isUnsubstitutedGenericClassType(genericClass: GenericClass): Boolean {
    return genericClass.typeParameters.any {
        it.substitutionType == null || isUnsubstitutedType(it.substitutionType!!)
    }
}

val outputTypeOOO = CustomExpressionValueic() { tokens, startIndex, endIndex, thisExpression ->
    return@CustomExpressionValueic outputType(tokens, startIndex, endIndex, thisExpression)
}

fun outputType(tokens: List<Char>, startIndex: Int, endIndex: Int, thisExpression: CustomExpressionValueic): ExpressionResult? {
    val evaluation = evaluateExpressionValueic(classType, startIndex, tokens, endIndex) ?:
    evaluateExpressionValueic(referenceType, startIndex, tokens, endIndex) ?:
    evaluateExpressionValueic(functionType, startIndex, tokens, endIndex) ?:
    evaluateExpressionValueic(genericClassType, startIndex, tokens, endIndex) ?:
    evaluateExpressionValueic(arrayType, startIndex, tokens, endIndex)
    if (evaluation != null) {
        return ContentExpressionResult(ExpressionResult(thisExpression, evaluation.range, evaluation.nextTokenIndex), evaluation)
    } else {
        return null
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "SomeClass<Int, Int>".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(outputTypeO))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleOutputType(currentGeneration)
        }
    }
}
