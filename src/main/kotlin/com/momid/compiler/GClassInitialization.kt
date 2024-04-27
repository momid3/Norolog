package com.momid.compiler

import com.momid.compiler.discover.GivenClass
import com.momid.compiler.discover.discoverClass
import com.momid.compiler.output.*
import com.momid.compiler.packaging.FilePackage
import com.momid.compiler.standard_library.handleBuiltInClassInitialization
import com.momid.parser.expression.*

val ciName by lazy {
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })
}

val ciParameters by lazy {
    oneOrZero(
        splitByNW(complexExpression, ",")
    )
}

val ci by lazy {
    ciName["ciName"] + spaces + oneOrZero(insideOf('<', '>') {
        typeParameters
    }["ciTypeParameters"])["ciTypeParameters"] + insideOf('(', ')') {
        ciParameters
    }["ciParameters"]
}

fun ExpressionResultsHandlerContext.handleCIParsing(): Result<CIParsing> {
    with(this.expressionResult) {
        val className = this["ciName"]
        val ciParameters = this["ciParameters"].continuing?.continuing?.asMulti()?.map {
            val parameter = it
            parameter
        }.orEmpty()

        val ciTypeParameters = this["ciTypeParameters"].continuing?.continuing?.asMulti()?.map {
            val typeParameter = it.continuing!!
            typeParameter
        }.orEmpty()

        return Ok(CIParsing(parsing(className), ciParameters.map { parsing(it) }, ciTypeParameters.map { parsing(it) }))
    }
}

fun ExpressionResultsHandlerContext.handleCI(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    val ciParsing = handleCIParsing().okOrReport {
        return it.to()
    }

    with(ciParsing) {
        println("ci " + this@handleCI.expressionResult.tokens)
        val className = this.className.tokens
        var resolvedClass = resolveType(className, currentGeneration)

        if (resolvedClass == null) {
            val discoveredClass = discoverClass(GivenClass(this.className.tokens, ""), currentGeneration)
            if (discoveredClass.isNotEmpty()) {
                resolvedClass = discoveredClass[0]
            }
        }

        if (resolvedClass == null) {
            return Error("unresolved class: " + className, this.className.range)
        }

        val existingTypeParameterMappings = HashMap<GenericTypeParameter, OutputType>()

        if (resolvedClass is GenericClass) {
            resolvedClass.unsubstituted = false

            if (this.typeParameters.isNotEmpty()) {
                val typeParameterEvaluations = this.typeParameters.map {
                    val outputType = continueWithOne(
                        it.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                    outputType
                }

                if (typeParameterEvaluations.size != resolvedClass.typeParameters.size) {
                    return Error("expected " + resolvedClass.typeParameters.size + " type parameters ", this@handleCI.expressionResult.range)
                }

                typeParameterEvaluations.forEachIndexed { index, outputType ->
                    existingTypeParameterMappings[GenericTypeParameter(this.typeParameters[index].tokens)] = outputType
                    resolvedClass.typeParameters[index].substitutionType = outputType
                }
            }

            val parameterEvaluations = ArrayList<String>()

            var cStruct: CStruct

            this.parameters.forEachIndexed { index, parameter ->
                val (evaluation, outputType) = continueWithOne(
                    parameter.expressionResult!!,
                    complexExpression
                ) { handleComplexExpression(currentGeneration) }.okOrReport {
                    return it.to()
                }

                parameterEvaluations.add(evaluation)

                val (typesMatch, substitutions) = typesMatch(outputType, resolvedClass.variables[index].type)
                if (!typesMatch) {
                    return Error("type mismatch: " + parameter.tokens + ". expected " + resolvedClass.variables[index].type.text + " got " + outputType.text, parameter.range)
                }
            }

            with(handleBuiltInClassInitialization(resolvedClass, parameterEvaluations, currentGeneration)) {
                if (this != null) {
                    return this
                }
            }

            cStruct = createGenericClassIfNotExists(currentGeneration, resolvedClass)

            val output = cStructInitialization(cStruct.name, cStruct.variables.mapIndexed { index, cStructVariable ->
                Pair(cStructVariable.name, parameterEvaluations[index])
            })

            return Ok(Pair(output, ClassType(resolvedClass)))
        } else {
            val cStruct = resolveClass(resolvedClass, currentGeneration)

            val parameterEvaluations = ArrayList<String>()
            this.parameters.forEachIndexed { index, parameter ->
                val (evaluation, outputType) = continueWithOne(parameter.expressionResult!!, complexExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
                    return it.to()
                }

                parameterEvaluations.add(evaluation)

                if (!typesMatch(outputType, resolvedClass.variables[index].type).first) {
                    return Error("type mismatch: " + parameter.tokens, parameter.range)
                }
            }

            with(handleBuiltInClassInitialization(resolvedClass, parameterEvaluations, currentGeneration)) {
                if (this != null) {
                    return this
                }
            }

            val output = cStructInitialization(cStruct.name, cStruct.variables.mapIndexed { index, cStructVariable ->
                Pair(cStructVariable.name, parameterEvaluations[index])
            })

            return Ok(Pair(output, ClassType(resolvedClass)))
        }
    }
}

class CIParsing(val className: Parsing, val parameters: List<Parsing>, val typeParameters: List<Parsing>)

fun typesMatch(outputType: OutputType, expectedType: OutputType): Pair<Boolean, HashMap<GenericTypeParameter, OutputType>> {
    if (expectedType is ClassType && expectedType.outputClass is GenericClass) {
        if (outputType is ClassType && outputType.outputClass is GenericClass) {
            println("here")
            if (!classEquals(outputType.outputClass, expectedType.outputClass)) {
                return Pair(false, hashMapOf())
            }
            if (outputType.outputClass.typeParameters.size != expectedType.outputClass.typeParameters.size) {
                return Pair(false, hashMapOf())
            }

            val substitutions = HashMap<GenericTypeParameter, OutputType>()
            outputType.outputClass.typeParameters.forEachIndexed { index, parameter ->
                if (expectedType.outputClass.typeParameters[index].substitutionType != null) {
                    val (typesMatch, substitution) = typesMatch(
                        parameter.substitutionType!!,
                        expectedType.outputClass.typeParameters[index].substitutionType!!
                    )
                    if (!typesMatch) {
                        return Pair(false, hashMapOf())
                    }
                    substitution.forEach { (genericTypeParameter, outputType) ->
                        if (substitutions[genericTypeParameter] != null && substitutions[genericTypeParameter] != outputType) {
                            return Pair(false, hashMapOf())
                        } else {
                            substitutions[genericTypeParameter] = outputType
                        }
                    }
                } else {
                    val genericTypeParameter = expectedType.outputClass.typeParameters[index]
                    val substitutionType = parameter.substitutionType
                    substitutions[genericTypeParameter] = substitutionType ?: throw (Throwable("provided outputType should have had a substituted type"))
                    expectedType.outputClass.typeParameters[index].substitutionType = substitutionType
                    if (substitutions[genericTypeParameter] != null && substitutions[genericTypeParameter] != substitutionType) {
                        return Pair(false, hashMapOf())
                    } else {
                        substitutions[genericTypeParameter] = substitutionType
                    }
                }
            }

            return Pair(true, substitutions)
        } else {
            return Pair(false, hashMapOf())
        }
    }

    else if (expectedType is TypeParameterType) {
        val genericTypeParameter = expectedType.genericTypeParameter
        val substitution = genericTypeParameter.substitutionType
        if (substitution == null) {
            genericTypeParameter.substitutionType = outputType
            return Pair(true, hashMapOf(genericTypeParameter to outputType))
        } else if (!typesMatch(outputType, substitution).first) {
            return Pair(false, hashMapOf())
        } else {
            return Pair(true, hashMapOf())
        }
    }

    else if (expectedType is ReferenceType) {
        if (outputType is ReferenceType) {
            return typesMatch(outputType.actualType, expectedType.actualType)
        }
    }

    else if (expectedType is FunctionType) {
        if (outputType is FunctionType) {
            val substitutions = HashMap<GenericTypeParameter, OutputType>()
            outputType.outputFunction.parameters.forEachIndexed { index, parameter ->
                val (typesMatch, substitution) = typesMatch(parameter.type, expectedType.outputFunction.parameters[index].type)
                if (!typesMatch) {
                    return Pair(false, hashMapOf())
                }
                substitution.forEach { (genericTypeParameter, outputType) ->
                    if (substitutions[genericTypeParameter] != null && substitutions[genericTypeParameter] != outputType) {
                        return Pair(false, hashMapOf())
                    } else {
                        substitutions[genericTypeParameter] = outputType
                    }
                }
            }
            outputType.outputFunction.returnType.apply {
                val (typesMatch, substitution) = typesMatch(this, expectedType.outputFunction.returnType)
                if (!typesMatch) {
                    return Pair(false, hashMapOf())
                }
                substitution.forEach { (genericTypeParameter, outputType) ->
                    if (substitutions[genericTypeParameter] != null && substitutions[genericTypeParameter] != outputType) {
                        return Pair(false, hashMapOf())
                    } else {
                        substitutions[genericTypeParameter] = outputType
                    }
                }
            }
            return Pair(true, substitutions)
        }

        if (outputType is EarlyLambda) {
            return Pair(outputType.parametersSize == expectedType.outputFunction.parameters.size, hashMapOf())
        }
    }

    else if (expectedType is ArrayType) {
        if (outputType is ArrayType) {
            val typesMatch = typesMatch(outputType.itemsType, expectedType.itemsType)
            return Pair(typesMatch.first && outputType.size == expectedType.size, typesMatch.second)
        }
    } else if (expectedType is ClassType) {
        if (outputType is ClassType) {
            return Pair(outputType.outputClass == expectedType.outputClass, hashMapOf())
        }
    } else if (expectedType == norType) {
        if (outputType == norType) {
            return Pair(true, hashMapOf())
        }
    }

    return Pair(false, hashMapOf())
}

fun main() {
    val currentGeneration = CurrentGeneration("", FilePackage("", ""))
    val text = ("List<Int>(0);").toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(ci))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleCI(currentGeneration)
        }
    }
}
