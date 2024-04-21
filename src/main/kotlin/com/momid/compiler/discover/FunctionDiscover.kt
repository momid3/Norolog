package com.momid.compiler.discover

import com.momid.compiler.*
import com.momid.compiler.output.Class
import com.momid.compiler.output.Function
import com.momid.compiler.output.GenericClass
import com.momid.compiler.packaging.readFilesRecursively
import com.momid.compiler.terminal.blue
import com.momid.compiler.terminal.printError
import com.momid.parser.expression.*

val rootDirectory = "C:\\Users\\moham\\Desktop\\compilation\\MomidCompilation"
val mainFilePackage = "source"

fun discoverFunction(function: FunctionCallEvaluating, currentGeneration: CurrentGeneration): List<Function> {
    val foundFunctions = ArrayList<Function>()
    readFilesRecursively(rootDirectory, mainFilePackage) { isMainFile, fileContent ->
        val codeText = fileContent
        val text = codeText.toList()
        val finder = ExpressionFinder()
        finder.registerExpressions(listOf(classFunction))

        finder.startDiscover(text).forEach {
            handleExpressionResult(finder, it, text) {
                this.expressionResult.isOf(classFunction) {
                    val classFunctionParsing = handleClassFunctionParsing().okOrReport {
                        return@handleExpressionResult it.to()
                    }
                    with(classFunctionParsing) {
                        if (
                            this.name.tokens == function.name.tokens &&
                            this.parameters.size == function.parameters.size &&
                            (this.receiverType == null) == (function.receiver == null)
                            ) {
                            println(blue("discovered function matches with expected function call " + function.name.tokens))
                            val resolvedFunction = handleClassFunction(currentGeneration, true).okOrReport {
                                return@handleExpressionResult it.to()
                            }
                            foundFunctions.add(resolvedFunction)
                        }
                    }
                }
                return@handleExpressionResult Ok(true)
            }
        }

        currentGeneration.errors.apply {
            if (this.isNotEmpty()) {
                println("program contains errors: ")
                println()
                this.forEach {
                    printError(it.error + ": " + codeText.slice(it.range.first until it.range.last))
                }
            }
        }
    }
    return foundFunctions
}

fun discoverClass(klass: GivenClass, currentGeneration: CurrentGeneration): List<Class> {
    val foundFunctions = ArrayList<Class>()
    readFilesRecursively(rootDirectory, mainFilePackage) { isMainFile, fileContent ->
        val codeText = fileContent
        val text = codeText.toList()
        val finder = ExpressionFinder()
        finder.registerExpressions(listOf(gClass))

        finder.startDiscover(text).forEach {
            handleExpressionResult(finder, it, text) {
                this.expressionResult.isOf(gClass) {
                    val classParsing = handleClassDeclarationParsing().okOrReport {
                        return@handleExpressionResult it.to()
                    }
                    with(classParsing) {
                        if (this.name.tokens == klass.name) {
                            println(blue("discovered class matches with expected class call " + klass.name))
                            var resolvedClass = handleClassDeclaration(currentGeneration, true).okOrReport {
                                return@handleExpressionResult it.to()
                            }
                            if (resolvedClass is GenericClass) {
                                resolvedClass = resolvedClass.clone()
                            }
                            foundFunctions.add(resolvedClass)
                        }
                    }
                }
                return@handleExpressionResult Ok(true)
            }
        }

        currentGeneration.errors.apply {
            if (this.isNotEmpty()) {
                println("program contains errors: ")
                println()
                this.forEach {
                    printError(it.error + ": " + codeText.slice(it.range.first until it.range.last))
                }
            }
        }
    }

    foundFunctions.addAll(discoverCClassMapping(klass, currentGeneration))

    return foundFunctions
}

fun discoverCClassMapping(klass: GivenClass, currentGeneration: CurrentGeneration): List<Class> {
    val foundFunctions = ArrayList<Class>()
    readFilesRecursively(rootDirectory, mainFilePackage) { isMainFile, fileContent ->
        val codeText = fileContent
        val text = codeText.toList()
        val finder = ExpressionFinder()
        finder.registerExpressions(listOf(cClassMapping))

        finder.startDiscover(text).forEach {
            handleExpressionResult(finder, it, text) {
                this.expressionResult.isOf(cClassMapping) {
                    val className = it["className"].tokens
                    with(className) {
                        if (this == klass.name) {
                            println(blue("discovered class matches with expected class call " + klass.name))
                            var resolvedClass = handleCClassMapping(currentGeneration, true).okOrReport {
                                return@handleExpressionResult it.to()
                            }
                            if (resolvedClass is GenericClass) {
                                resolvedClass = resolvedClass.clone()
                            }
                            foundFunctions.add(resolvedClass)
                        }
                    }
                }
                return@handleExpressionResult Ok(true)
            }
        }

        currentGeneration.errors.apply {
            if (this.isNotEmpty()) {
                println("program contains errors: ")
                println()
                this.forEach {
                    printError(it.error + ": " + codeText.slice(it.range.first until it.range.last))
                }
            }
        }
    }
    return foundFunctions
}

/***
 * represents a class name and package when encountered in code. used to resolve the class using it later.
 */
class GivenClass(val name: String, val declarationPackage: String)
