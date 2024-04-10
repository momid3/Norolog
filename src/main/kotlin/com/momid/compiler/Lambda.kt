package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.output.Function
import com.momid.parser.expression.*
import com.momid.parser.not

val lambdaParameters =
    oneOrZero((splitByNW(one(spaces + className["parameterName"] + spaces), ",")["parameters"] + spaces + !"#")["lambdaParameters"])

val lambda =
    insideOf('{', '}') {
        lambdaParameters["lambdaParameters"] + anything["insideLambda"]
    }

fun ExpressionResultsHandlerContext.handleLambdaParsing(): Result<LambdaParsing> {
    with(this.expressionResult) {
        val inside = this.continuing {
            return Error("there is an issue inside lambda " + it.tokens, it.range)
        }
        val parameters = inside["lambdaParameters"].continuing?.asMulti()?.map {
            val parameter = it
            parameter
        }.orEmpty()

        val codeInside = inside["insideLambda"]

        return Ok(LambdaParsing(parameters.map { it.parsing }, codeInside.parsing, this.parsing))
    }
}

fun ExpressionResultsHandlerContext.handleEarlyLambda(currentGeneration: CurrentGeneration): Result<EarlyLambda> {
    val lambdaParsing = handleLambdaParsing().okOrReport {
        return it.to()
    }
    with(lambdaParsing) {
        return Ok(EarlyLambda(this.parameters.size))
    }
}

fun ExpressionResultsHandlerContext.handleLambda(currentGeneration: CurrentGeneration, parameter: FunctionParameter): Result<Pair<String, OutputType>> {
    val lambdaParsing = handleLambdaParsing().okOrReport {
        return it.to()
    }
    with(lambdaParsing) {
        val lambdaFunctionName = "lambda_" + currentGeneration.createCFunctionName()
        val functionParameters = ArrayList<FunctionParameter>()
        val function = Function(lambdaFunctionName, listOf(), norType, IntRange.EMPTY)

        val parameterExpectedLambda = parameter.type as FunctionType

        val lambdaScope = Scope()
        lambdaScope.scopeContext = LambdaContext(function)

        for (index in parameterExpectedLambda.outputFunction.parameters.indices) {
            val parameterExpectedLambdaParameter = parameterExpectedLambda.outputFunction.parameters[index]
            functionParameters.add(
                FunctionParameter(this.parameters[index].tokens, parameterExpectedLambdaParameter.type, parameterExpectedLambdaParameter.isReferenceParameter)
            )
        }

        parameterExpectedLambda.outputFunction.parameters.forEachIndexed { index, lambdaParameter ->
            lambdaScope.variables.add(
                VariableInformation(
                    this.parameters[index].tokens,
                    resolveType(lambdaParameter.type, currentGeneration),
                    "",
                    this.parameters[index].tokens,
                    lambdaParameter.type,
                    functionParameters[index]
                )
            )
        }

        val (lambdaCCode, returnType) = continueStraight(this.codeRange.expressionResult) { handleCodeBlockWithOutputType(currentGeneration, lambdaScope) }.okOrReport {
            return it.to()
        }

        println("lambda return type " + returnType.text)

        function.returnType = returnType

        val cFunction = CFunction(lambdaFunctionName, functionParameters.map {
            CFunctionParameter(it.name, resolveType(it.type, currentGeneration))
        }, resolveType(parameterExpectedLambda.outputFunction.returnType, currentGeneration), lambdaCCode)

        currentGeneration.functionDeclarationsGeneratedSource += cFunction(cFunction.name, cFunction.parameters.map {
            cTypeAndVariableName(it.type, it.name)
        }, cTypeName(cFunction.returnType), cFunction.codeText)

        return Ok(Pair(lambdaFunctionName, FunctionType(function, cFunction)))
    }
}

class EarlyLambda(
    val parametersSize: Int,
    val returnType: OutputType? = null
) : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is FunctionType && other.outputFunction.parameters.size == parametersSize
    }

    override fun hashCode(): Int {
        var result = parametersSize
        result = 31 * result + (returnType?.hashCode() ?: 0)
        return result
    }
}

class Lambda()

class LambdaParsing(
    val parameters: List<Parsing>,
    val codeRange: Parsing,
    val parsing: Parsing
)

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = ("{ showParameter #\n" +
            "    return 3\n" +
            "}").toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(lambda))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleLambda(currentGeneration, FunctionParameter("", norType))
        }
    }
}
