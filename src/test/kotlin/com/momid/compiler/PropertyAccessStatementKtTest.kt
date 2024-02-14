package com.momid.compiler

import com.momid.parser.expression.ExpressionFinder
import com.momid.parser.expression.handleExpressionResult
import org.junit.jupiter.api.Test

class PropertyAccessStatementKtTest {

    @Test
    fun propertyAccessStatement() {
        val currentGeneration = CurrentGeneration()
        val text = "keep.change(333) ;".toList()
        val finder = ExpressionFinder()
        finder.registerExpressions(listOf(propertyAccessStatement))
        finder.start(text).forEach {
            handleExpressionResult(finder, it, text) {
                println(this.tokens.joinToString(""))
                assert(this.tokens.joinToString("").isNotEmpty()) {
                    "evaluation against propertyAccessStatement did not work"
                }
                handlePropertyAccessStatement(currentGeneration)
            }
        }
    }
}