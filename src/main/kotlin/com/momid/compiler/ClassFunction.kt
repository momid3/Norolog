package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.parser.expression.*
import com.momid.parser.not

val parameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val functionReturnType =
    oneOrZero(spaces + !":" + spaces + outputTypeO["returnType"], "functionReturnType")

val classFunctionTypeParameters =
    oneOrZero(insideOf('<', '>') {
        typeParameters["typeParameter"]
    }, "typeParameters")

val classFunction =
    !"fun" + space + classFunctionTypeParameters["typeParameters"] + oneOrZero(outputTypeO["receiverType"], "receiverType") +
            !"." + className["functionName"] + insideOf('(', ')') {
        oneOrZero(splitBy(parameter, ","))["functionParameters"]
    } + spaces + functionReturnType["functionReturnType"] + spaces + insideOf('{', '}') {
        anything["functionInside"]
    }

fun ExpressionResultsHandlerContext.handleClassFunctionParsing(): Result<ClassFunctionParsing> {
    with(this.expressionResult) {
        val functionName = this["functionName"].parsing
        val receiverType = this["receiverType"].continuing?.parsing
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
        val functionInside = this["functionInside"].continuing!!.parsing
        return Ok(ClassFunctionParsing(functionName, functionParameters, typeParameters, functionReturnType, receiverType, functionInside))
    }
}

fun ExpressionResultsHandlerContext.handleClassFunction(currentGeneration: CurrentGeneration): Result<Boolean> {
    val classFunctionParsing = handleClassFunctionParsing().okOrReport {
        return it.to()
    }

    with(classFunctionParsing) {
        if (receiverType != null) {
            val isGeneric = typeParameters.isNotEmpty()

            val receiverType = continueWithOne(receiverType.expressionResult, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }

            val functionParameters = parameters.map {
                val parameterType = continueWithOne(it.type.expressionResult, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                    return it.to()
                }
                FunctionParameter(it.name.tokens, parameterType)
            }

            val returnType = if (returnType != null) {
                continueWithOne(returnType.expressionResult, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                    return it.to()
                }
            } else {
                norType
            }

            if (!isGeneric) {
                val function = ClassFunction(
                    receiverType,
                    Function(
                        name.tokens,
                        functionParameters,
                        returnType,
                        functionInside.range
                    )
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(function)

                val cFunction = CFunction(
                    function.name,
                    (function.parameters.map {
                        CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
                    } as ArrayList).apply {
                        this.add(0, CFunctionParameter("this_receiver", resolveType(function.receiverType, currentGeneration)))
                    },
                    resolveType(function.returnType, currentGeneration),
                    ""
                )

                function.parameters.forEachIndexed { index, functionParameter ->
                    val variableInformation = VariableInformation(
                        cFunction.parameters[index + 1].name,
                        cFunction.parameters[index + 1].type,
                        "",
                        functionParameter.name,
                        functionParameter.type
                    )
                    functionScope.variables.add(variableInformation)
                }

                functionScope.variables.add(
                    VariableInformation(
                        cFunction.parameters[0].name,
                        cFunction.parameters[0].type,
                        "",
                        "this",
                        function.receiverType
                    )
                )

                currentGeneration.functionsInformation.functionsInformation[function] = cFunction

                val cFunctionCode = continueStraight(functionInside.expressionResult) { handleCodeBlock(currentGeneration, functionScope) }.okOrReport {
                    return it.to()
                }

                cFunction.codeText = cFunctionCode

                currentGeneration.functionDeclarationsGeneratedSource += cFunction(cFunction.name, cFunction.parameters.map {
                    cTypeAndVariableName(it.type, it.name)
                }, cTypeName(cFunction.returnType), cFunction.codeText.trim()) + "\n"

                return Ok(true)
            } else {
                val classFunction = ClassFunction(
                    receiverType,
                    Function(
                        name.tokens,
                        functionParameters,
                        returnType,
                        functionInside.range
                    )
                )
                val function = GenericFunction(
                    typeParameters.map { GenericTypeParameter(it.tokens) },
                    classFunction
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(function)

                val cFunction = CFunction(
                    function.name,
                    (function.parameters.map {
                        CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
                    } as ArrayList).apply {
                        this.add(0, CFunctionParameter("this_receiver", resolveType(classFunction.receiverType, currentGeneration)))
                    },
                    resolveType(function.returnType, currentGeneration),
                    ""
                )

                function.parameters.forEachIndexed { index, functionParameter ->
                    val variableInformation = VariableInformation(
                        cFunction.parameters[index + 1].name,
                        cFunction.parameters[index + 1].type,
                        "",
                        functionParameter.name,
                        functionParameter.type
                    )
                    functionScope.variables.add(variableInformation)
                }

                functionScope.variables.add(
                    VariableInformation(
                        cFunction.parameters[0].name,
                        cFunction.parameters[0].type,
                        "",
                        "this",
                        classFunction.receiverType
                    )
                )

                currentGeneration.functionsInformation.functionsInformation[function] = cFunction

                val cFunctionCode = continueStraight(functionInside.expressionResult) { handleCodeBlock(currentGeneration, functionScope) }.okOrReport {
                    return it.to()
                }

                cFunction.codeText = cFunctionCode

                currentGeneration.functionDeclarationsGeneratedSource += cFunction(cFunction.name, cFunction.parameters.map {
                    cTypeAndVariableName(it.type, it.name)
                }, cTypeName(cFunction.returnType), cFunction.codeText.trim()) + "\n"

                return Ok(true)
            }
        } else {
            return Ok(true)
        }
    }
}

class ClassFunctionParameterParsing(val name: Parsing, val type: Parsing)

class ClassFunctionParsing(
    val name: Parsing,
    val parameters: List<ClassFunctionParameterParsing>,
    val typeParameters: List<Parsing>,
    val returnType: Parsing?,
    val receiverType: Parsing?,
    val functionInside: Parsing
)
