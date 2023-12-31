package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*

val ciName =
    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })

val ciParameters =
    oneOrZero(
        splitBy(anything, ",")
    )

val ci =
    ciName["ciName"] + spaces + insideOf('(', ')') {
        ciParameters["ciParameters"]
    }

fun ExpressionResultsHandlerContext.handleCIParsing(): Result<CIParsing> {
    with(this.expressionResult) {
        val className = this["ciName"]
        val ciParameters = this["ciParameters"].continuing?.continuing?.asMulti()?.map {
            val parameter = it.continuing!!
            parameter
        }.orEmpty()

        return Ok(CIParsing(parsing(className), ciParameters.map { parsing(it) }))
    }
}

fun ExpressionResultsHandlerContext.handleCI(currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>> {
    val ciParsing = handleCIParsing().okOrReport {
        return it.to()
    }

    with(ciParsing) {
        val className = this.className.tokens
        val resolvedClass = resolveType(className, currentGeneration) ?:
        return Error("unresolved class: " + className, this.className.range)

        if (resolvedClass is GenericClass) {
            resolvedClass.unsubstituted = false

            val cStructVariables = ArrayList<CStructVariable>()
            val parameterEvaluations = ArrayList<String>()

            var cStruct = CStruct(currentGeneration.createCStructName(), cStructVariables)

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
                    return Error("type mismatch: " + parameter.tokens + ". expected " + resolvedClass.variables[index].type + " got " + outputType, parameter.range)
                }
            }

            val alreadyExists = currentGeneration.classesInformation.classes.entries.find {
                it.key == resolvedClass
            } != null

            resolvedClass.variables.forEach {
                cStructVariables.add(CStructVariable(it.name, resolveType(it.type, currentGeneration)))
            }

            if (alreadyExists) {
                cStruct = currentGeneration.classesInformation.classes.entries.find {
                    it.key == resolvedClass
                }!!.value!!
            } else {
                currentGeneration.classesInformation.classes[resolvedClass] = cStruct

                currentGeneration.globalDefinitionsGeneratedSource += cStruct(cStruct.name, cStruct.variables.map { Pair(it.name, cTypeName(it.type)) })
            }

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

            val output = cStructInitialization(cStruct.name, cStruct.variables.mapIndexed { index, cStructVariable ->
                Pair(cStructVariable.name, parameterEvaluations[index])
            })

            return Ok(Pair(output, ClassType(resolvedClass)))
        }
    }
}

class CIParsing(val className: Parsing, val parameters: List<Parsing>)

fun typesMatch(outputType: OutputType, expectedType: OutputType): Pair<Boolean, HashMap<GenericTypeParameter, OutputType>> {
    if (expectedType is ClassType && expectedType.outputClass is GenericClass) {
        if (outputType is ClassType && outputType.outputClass is GenericClass) {
            if (outputType.outputClass != expectedType.outputClass) {
                return Pair(false, hashMapOf())
            }
            if (outputType.outputClass.typeParameters.size != expectedType.outputClass.typeParameters.size) {
                return Pair(false, hashMapOf())
            }

            val substitutions = HashMap<GenericTypeParameter, OutputType>()
            outputType.outputClass.typeParameters.forEachIndexed { index, parameter ->
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

    else if (expectedType is ClassType) {
        if (outputType is ClassType) {
            return Pair(outputType.outputClass == expectedType.outputClass, hashMapOf())
        }
    }

    else if (expectedType == norType) {
        if (outputType == norType) {
            return Pair(true, hashMapOf())
        }
    }

    return Pair(false, hashMapOf())
}
