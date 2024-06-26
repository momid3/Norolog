package com.momid.compiler

import com.momid.compiler.output.Class
import com.momid.compiler.output.Function
import com.momid.compiler.output.OutputType
import com.momid.compiler.output.text
import com.momid.parser.expression.*
import com.momid.parser.not

val returnStatement =
    !"return" + space + wanting(complexExpression["returnExpression"])

fun ExpressionResultsHandlerContext.handleReturnStatement(currentGeneration: CurrentGeneration): Result<OutputType> {
    with(this.expressionResult) {
        val returnExpression = this["returnExpression"].continuing {
            println("expected expression, found: " + it.tokens())
            return Error("expected expression, found: " + it.tokens(), it.range)
        }
        val (evaluation, outputType) = continueStraight(returnExpression) { handleComplexExpression(currentGeneration) }.okOrReport {
            println(it.error)
            return it.to()
        }

        if (currentGeneration.currentScope.scopeContext is FunctionContext) {
            val functionReturnType = (currentGeneration.currentScope.scopeContext as FunctionContext).function.returnType
            val typesMatch = typesMatch(outputType, functionReturnType).first || typesMatch(functionReturnType, outputType).first
            if (!typesMatch) {
                println("return type of function is " + functionReturnType + " but found " + outputType)
                return Error("return type of function is " + functionReturnType.text + "but found " + outputType.text, this.range)
            }
        }

        currentGeneration.currentScope.generatedSource += returnStatement(evaluation)

        return Ok(outputType)
    }
}

open class Context()

class ClassContext(val outputClass: Class): Context()

class FunctionContext(val function: Function): Context()

class LambdaContext(val lambda: Function): Context()

class ForLoopContext(): Context()

class LoopContext(): Context()

class IfContext(): Context()

class CExpressionContext(): Context()
