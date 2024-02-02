package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val parameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val functionReturnType =
    oneOrZero(spaces + !":" + spaces + outputTypeO["returnType"])

val classFunctionTypeParameters =
    oneOrZero(insideOf('<', '>') {
        typeParameters["typeParameter"]
    }, "typeParameters")

val classFunction =
    !"fun" + space + classFunctionTypeParameters["typeParameters"] + outputTypeO["receiverType"] +
            !"." + className["functionName"] + insideOf('(', ')') {
        oneOrZero(splitBy(parameter, ","))["functionParameters"]
    } + spaces + functionReturnType["functionReturnType"] + spaces + insideOf('{', '}') {
        anything["functionInside"]
    }

fun ExpressionResultsHandlerContext.handleClassFunctionParsing(): Result<ClassFunctionParsing> {
    with(this.expressionResult) {
        val functionName = this["functionName"].parsing
        val receiverType = this["receiverType"].parsing
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
        return Ok(ClassFunctionParsing(functionName, functionParameters, typeParameters, functionReturnType, receiverType, functionInside))
    }
}

fun ExpressionResultsHandlerContext.handleClassFunction(currentGeneration: CurrentGeneration): Result<Boolean> {
    val classFunctionParsing = handleClassFunctionParsing().okOrReport {
        return it.to()
    }

    with(classFunctionParsing) {
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
                name.tokens,
                receiverType,
                functionParameters,
                returnType,
                functionInside.range
            )

            val scope = Scope()
            scope.scopeContext = FunctionContext(function)
            val functionScope = currentGeneration.createScope(scope)

            val cFunctionCode = continueStraight(functionInside.expressionResult) { handleCodeBlock(currentGeneration, functionScope) }.okOrReport {
                return it.to()
            }

            val cFunction = CFunction(
                function.name,
                function.parameters.map {
                    CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
                },
                resolveType(function.returnType, currentGeneration),
                cFunctionCode
            )

            currentGeneration.functionsInformation.functionsInformation[function] = cFunction

            currentGeneration.currentScope.generatedSource += cFunction(cFunction.name, cFunction.parameters.map {
                cTypeAndVariableName(it.type, it.name)
            }, cTypeName(cFunction.returnType), cFunction.codeText)

            return Ok(true)
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
    val receiverType: Parsing,
    val functionInside: Parsing
)
