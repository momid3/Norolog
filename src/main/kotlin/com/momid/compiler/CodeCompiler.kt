package com.momid.compiler

import com.momid.compiler.output.Scope
import com.momid.parser.expression.ExpressionFinder
import com.momid.parser.expression.handleExpressionResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

fun compile(codeText: String): String {
    val text = codeText.toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(statementsExp))

    val currentGeneration = CurrentGeneration()
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleStatements(currentGeneration)
        }
    }

//    println(currentGeneration.generatedSource)

    return wholeProgram(currentGeneration.generatedSource)
}

fun compileFromSource() {
    val codeText = readResourceFileContents("source.momid")
    val compiledCode = compile(codeText)
    println(compiledCode)
    createSource(compiledCode)
    runGeneratedSource()
}

fun readMomidFileContents(filePath: String): String {
    if (!filePath.endsWith(".momid")) {
        throw IllegalArgumentException("Invalid file type. File must have a .momid extension.")
    }

    val path = Paths.get(filePath)
    return Files.readString(path, StandardCharsets.UTF_8)
}

fun readResourceFileContents(fileName: String): String {
    val classLoader = Scope::class.java.classLoader
    val resource = classLoader.getResource(fileName)
        ?: throw IllegalArgumentException("Resource file not found. Make sure the path is correct.")

    return resource.readText(StandardCharsets.UTF_8)
}

fun main() {
    compileFromSource()
}
