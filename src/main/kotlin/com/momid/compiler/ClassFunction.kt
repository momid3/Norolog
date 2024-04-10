package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.parser.expression.*
import com.momid.parser.not

val parameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + oneOrZero((!"*")["isReferenceParameter"]) + outputTypeO["parameterType"] + spaces

val functionReturnType =
    oneOrZero((spaces + !":" + spaces + outputTypeO["returnType"])["functionReturnType"])

val classFunctionTypeParameters =
    oneOrZero(insideOf('<', '>') {
        typeParameters["typeParameters"]
    }["typeParameters"], "typeParameters")

val classFunction =
    !"fun" + space + classFunctionTypeParameters["typeParameters"] + spaces + oneOrZero(
        one(outputTypeO["receiverType"] + !".")["receiverType"],
        "receiverType"
    )["receiverType"] + className["functionName"] + insideOf('(', ')') {
        oneOrZero(splitByNW(parameter, ","))
    }["functionParameters"] + spaces + functionReturnType["functionReturnType"] + spaces + insideOf('{', '}') {
        anything
    }["functionInside"]

fun ExpressionResultsHandlerContext.handleClassFunctionParsing(): Result<ClassFunctionParsing> {
    with(this.expressionResult) {
        val functionName = this["functionName"].parsing
        val receiverType = this["receiverType"].continuing?.parsing
        val functionParameters = this["functionParameters"].continuing?.continuing?.asMulti()?.map {
            val parameter = it
            val isReferenceParameter = parameter["isReferenceParameter"].continuing != null
            ClassFunctionParameterParsing(parameter["parameterName"].parsing, parameter["parameterType"].parsing, isReferenceParameter)
        }.orEmpty()
        val typeParameters = this["typeParameters"].continuing?.continuing?.asMulti()?.map {
            val typeParameter = it.continuing {
                return Error("expected type parameter, found " + it.tokens, it.range)
            }
            typeParameter.parsing
        }.orEmpty()
        val functionReturnType = this["functionReturnType"].continuing?.get("returnType")?.parsing
        val functionInside = this["functionInside"].continuing!!.parsing
        return Ok(
            ClassFunctionParsing(
                functionName,
                functionParameters,
                typeParameters,
                functionReturnType,
                receiverType,
                functionInside
            )
        )
    }
}

fun ExpressionResultsHandlerContext.handleClassFunction(currentGeneration: CurrentGeneration, discover: Boolean = false): Result<Function> {
    val classFunctionParsing = handleClassFunctionParsing().okOrReport {
        return it.to()
    }

    with(classFunctionParsing) {
        val isGeneric = typeParameters.isNotEmpty()

        if (!isGeneric) {
            if (this.receiverType != null) {

                val receiverType = continueWithOne(
                    this.receiverType.expressionResult,
                    outputTypeO
                ) { handleOutputType(currentGeneration) }.okOrReport {
                    return it.to()
                }

                val functionParameters = parameters.map {
                    val parameterType = continueWithOne(
                        it.type.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                    FunctionParameter(it.name.tokens, parameterType, it.isReferenceParameter)
                }

                val returnType = if (returnType != null) {
                    continueWithOne(
                        returnType.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                } else {
                    norType
                }

                val function = ClassFunction(
                    receiverType,
                    Function(
                        name.tokens,
                        functionParameters,
                        returnType,
                        functionInside.range,
                        discover
                    )
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(function)

                val cFunction = CFunction(
                    function.name,
                    (function.parameters.map {
                        if (it.isReferenceParameter) {
                            CFunctionParameter(it.name, CReferenceType(resolveType(it.type, currentGeneration)))
                        } else {
                            CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
                        }
                    } as ArrayList).apply {
                        this.add(
                            0,
                            CFunctionParameter("this_receiver", resolveType(function.receiverType, currentGeneration))
                        )
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
                        functionParameter.type,
                        functionParameter
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

                val discovered = checkIfFunctionAlreadyExists(function, currentGeneration).okOrReport {
                    return it.to()
                }

                if (discovered != null) {
                    return Ok(discovered)
                }

                currentGeneration.functionsInformation.functionsInformation[function] = cFunction

                val cFunctionCode = continueStraight(functionInside.expressionResult) {
                    handleCodeBlock(
                        currentGeneration,
                        functionScope
                    )
                }.okOrReport {
                    return it.to()
                }

                cFunction.codeText = cFunctionCode

                currentGeneration.functionDeclarationsGeneratedSource += cFunction(
                    cFunction.name,
                    cFunction.parameters.map {
                        cTypeAndVariableName(it.type, it.name)
                    },
                    cTypeName(cFunction.returnType),
                    cFunction.codeText.trim()
                ) + "\n"

                return Ok(function)
            } else {

                val functionParameters = parameters.map {
                    val parameterType = continueWithOne(
                        it.type.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                    FunctionParameter(it.name.tokens, parameterType, it.isReferenceParameter)
                }

                val returnType = if (returnType != null) {
                    continueWithOne(
                        returnType.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                } else {
                    norType
                }

                val function = Function(
                    name.tokens,
                    functionParameters,
                    returnType,
                    functionInside.range,
                    discover
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(function)

                val cFunction = CFunction(
                    function.name,
                    (function.parameters.map {
                        if (it.isReferenceParameter) {
                            CFunctionParameter(it.name, CReferenceType(resolveType(it.type, currentGeneration)))
                        } else {
                            CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
                        }
                    } as ArrayList),
                    resolveType(function.returnType, currentGeneration),
                    ""
                )

                function.parameters.forEachIndexed { index, functionParameter ->
                    val variableInformation = VariableInformation(
                        cFunction.parameters[index].name,
                        cFunction.parameters[index].type,
                        "",
                        functionParameter.name,
                        functionParameter.type,
                        functionParameter
                    )
                    functionScope.variables.add(variableInformation)
                }

                val discovered = checkIfFunctionAlreadyExists(function, currentGeneration).okOrReport {
                    return it.to()
                }

                if (discovered != null) {
                    return Ok(discovered)
                }

                currentGeneration.functionsInformation.functionsInformation[function] = cFunction

                val cFunctionCode = continueStraight(functionInside.expressionResult) {
                    handleCodeBlock(
                        currentGeneration,
                        functionScope
                    )
                }.okOrReport {
                    return it.to()
                }

                cFunction.codeText = cFunctionCode

                currentGeneration.functionDeclarationsGeneratedSource += cFunction(
                    cFunction.name,
                    cFunction.parameters.map {
                        cTypeAndVariableName(it.type, it.name)
                    },
                    cTypeName(cFunction.returnType),
                    cFunction.codeText.trim()
                ) + "\n"

                return Ok(function)
            }
        } else {
            if (this.receiverType != null) {
                println("receiver is not null")
                val classFunction = ClassFunction(
                    norType,
                    Function(
                        name.tokens,
                        listOf(),
                        norType,
                        functionInside.range,
                        discover
                    )
                )

                val function = GenericFunction(
                    typeParameters.map { GenericTypeParameter(it.tokens) },
                    classFunction
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(function)
                currentGeneration.createScope(functionScope)

                val receiverType = continueWithOne(
                    this.receiverType.expressionResult.also {
                        println("the receiver type is " + it.tokens)
                    },
                    outputTypeO
                ) { handleOutputType(currentGeneration) }.okOrReport {
                    return it.to()
                }

                val functionParameters = parameters.map {
                    val parameterType = continueWithOne(
                        it.type.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                    FunctionParameter(it.name.tokens, parameterType, it.isReferenceParameter)
                }

                val returnType = if (this.returnType != null) {
                    continueWithOne(
                        this.returnType.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                } else {
                    norType
                }

                classFunction.receiverType = receiverType
                function.parameters = functionParameters
                function.returnType = returnType

                val discovered = checkIfFunctionAlreadyExists(function, currentGeneration).okOrReport {
                    return it.to()
                }

                if (discovered != null) {
                    return Ok(discovered)
                }

                currentGeneration.functionsInformation.functionsInformation[function] = null

                currentGeneration.goOutOfScope()

                return Ok(function)
            } else {
                println("receiver is null")
                val function = Function(
                    name.tokens,
                    listOf(),
                    norType,
                    functionInside.range,
                    discover
                )
                val genericFunction = GenericFunction(
                    typeParameters.map { GenericTypeParameter(it.tokens) },
                    function
                )

                val functionScope = Scope()
                functionScope.scopeContext = FunctionContext(genericFunction)
                currentGeneration.createScope(functionScope)

                val functionParameters = parameters.map {
                    val parameterType = continueWithOne(
                        it.type.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                    FunctionParameter(it.name.tokens, parameterType, it.isReferenceParameter)
                }

                val returnType = if (this.returnType != null) {
                    continueWithOne(
                        this.returnType.expressionResult,
                        outputTypeO
                    ) { handleOutputType(currentGeneration) }.okOrReport {
                        return it.to()
                    }
                } else {
                    norType
                }

                genericFunction.parameters = functionParameters
                genericFunction.returnType = returnType

                val discovered = checkIfFunctionAlreadyExists(genericFunction, currentGeneration).okOrReport {
                    return it.to()
                }

                if (discovered != null) {
                    return Ok(discovered)
                }

                currentGeneration.functionsInformation.functionsInformation[genericFunction] = null

                currentGeneration.goOutOfScope()

                return Ok(genericFunction)
            }
        }
    }
}

class ClassFunctionParameterParsing(val name: Parsing, val type: Parsing, val isReferenceParameter: Boolean)

class ClassFunctionParsing(
    val name: Parsing,
    val parameters: List<ClassFunctionParameterParsing>,
    val typeParameters: List<Parsing>,
    val returnType: Parsing?,
    val receiverType: Parsing?,
    val functionInside: Parsing
)

fun ExpressionResultsHandlerContext.checkIfFunctionAlreadyExists(function: Function, currentGeneration: CurrentGeneration): Result<Function?> {
    val existingFunction = currentGeneration.functionsInformation.functionsInformation.entries.find {
        val currentFunction = it.key
        (it.key == function).also {
            println("expected function " + function.name + " current function " + currentFunction.name + " " + it)
        }
    }
    if (existingFunction != null && !existingFunction.key.discover) {
        return Error("function is already declared " + this.expressionResult.tokens, this.expressionResult.range)
    }
    if (existingFunction?.key?.discover == true) {
        return Ok(existingFunction.key)
    } else {
        return Ok(null)
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = ("fun <T> Keep<T>.genericFunction() {\n" +
            "    print(\"the parameter is\");\n" +
            "    print(this.keepValue);\n" +
            "}").toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(classFunction))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleClassFunction(currentGeneration)
        }
    }
}
