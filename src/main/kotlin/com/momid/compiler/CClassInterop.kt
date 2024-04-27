package com.momid.compiler

import com.momid.compiler.output.CStruct
import com.momid.compiler.output.Class
import com.momid.compiler.packaging.FilePackage
import com.momid.parser.expression.*
import com.momid.parser.not

val cClassMapping =
    !"class" + space + className["className"] + spaces + !"=" + spaces + className["cStructName"] + !";"

fun ExpressionResultsHandlerContext.handleCClassMapping(currentGeneration: CurrentGeneration, discover: Boolean = false): Result<Class> {
    with(this.expressionResult) {
        val className = this["className"]
        val cStructName = this["cStructName"]

        val outputClass = Class(className.tokens, listOf(), "")
        outputClass.discover = discover

        val cStruct = CStruct(cStructName.tokens, listOf())

        val discovered = checkIfClassAlreadyExists(outputClass, currentGeneration).okOrReport {
            return it.to()
        }

        if (discovered != null) {
            return Ok(discovered)
        }

        currentGeneration.classesInformation.classes[outputClass] = cStruct

        return Ok(outputClass)
    }
}

fun main() {
    val currentGeneration = CurrentGeneration("", FilePackage("", ""))
    val text = "class someClass = otherClass;".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(cClassMapping))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleCClassMapping(currentGeneration)
        }
    }
}
