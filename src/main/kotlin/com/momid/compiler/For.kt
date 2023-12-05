package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val forStatement =
    spaced {
        !"for" + insideOf('(', ')') {
            spaced {
                (wanting(variableNameO["variableName"], !"in") + "in"
                        + wanting(complexExpression["rangeStart"], !"until") + "until"
                        + wanting(complexExpression["rangeEnd"]))
            }["forRange"]
        } + (insideOf("forInside", '{', '}'))["forInside"]
    }

fun ExpressionResultsHandlerContext.handleForLoop(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(forStatement) {
        println("is for loop: " + it.tokens())
        var output = ""

        val forRange = it["forRange"].continuing {
            println("expected range of the for loop")
            return Error("expected range of the for loop", it.range)
        }

        val variableName = forRange["variableName"].continuing {
            println("expecting variable name")
            return Error("expecting variable name", it.range)
        }

        val rangeStart = forRange["rangeStart"].continuing {
            println("expecting expression for range start")
            return Error("expecting expression for range start", it.range)
        }

        val rangeEnd = forRange["rangeEnd"].continuing {
            println("expecting expression for range end")
            return Error("expecting expression for range end", it.range)
        }

        val rangeStartEvaluation = continueStraight(rangeStart) { handleComplexExpression(currentGeneration) }
        val rangeEndEvaluation = continueStraight(rangeEnd) { handleComplexExpression(currentGeneration) }
        val indexName = createVariableName()
        val indexVariable = VariableInformation(indexName, Type.Int, 0, variableName.tokens(), ClassType(outputInt))
        val scope = Scope()
        scope.variables.add(indexVariable)
        println("inside of for loop: " + it["forInside"].content.tokens())
        val insideForLoop = continueStraight(it["forInside"].content) { handleCodeBlock(currentGeneration, scope) }

        if (rangeStartEvaluation is Ok) {
            if (rangeEndEvaluation is Ok) {
                if (insideForLoop is Ok) {
                    output += forLoop(indexName, rangeStartEvaluation.ok.first, rangeEndEvaluation.ok.first, insideForLoop.ok)
                    currentGeneration.currentScope.generatedSource += output
                    return Ok("")
                }
            }
        }

        return Error("some error happened when evaluating for loop", it.range)
    }
    println("is not for loop")
    return Error("is not for loop", this.expressionResult.range)
}
