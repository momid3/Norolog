package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.packaging.readFilesRecursively
import com.momid.compiler.terminal.printError
import com.momid.parser.expression.ExpressionFinder
import com.momid.parser.expression.handleExpressionResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

fun compile(codeText: String): String {
    val text = codeText.toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(statementsExp))

    val currentGeneration = CurrentGeneration().apply {
        this.classesInformation.classes[Class(outputInt.name, arrayListOf())] = CStruct(Type.Int.name, arrayListOf())
        this.classesInformation.classes[Class(outputString.name, arrayListOf())] = CStruct(Type.CharArray.name, arrayListOf())
        this.classesInformation.classes[window] = CStruct("SDL_Window", listOf())
        this.classesInformation.classes[renderer] = CStruct("SDL_Renderer", listOf())
    }
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleStatements(currentGeneration)
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

//    println(currentGeneration.generatedSource)

    return wholeProgram(
        currentGeneration.currentScope.generatedSource,
        currentGeneration.globalDefinitionsGeneratedSource,
        currentGeneration.functionDeclarationsGeneratedSource
    )
}

fun compileFromSource() {
    val codeText = readResourceFileContents("source.momid")
    val compiledCode = compile(codeText)
    println(compiledCode)
    createSource(compiledCode)
    runGeneratedSource()
}

fun compileFromSource(rootDirectory: String, mainFilePackage: String) {
    readFilesRecursively(rootDirectory, mainFilePackage) { isMainFile, fileContent ->
        val codeText = fileContent
        val compiledCode = compile(codeText)
        println(compiledCode)
        createSource(compiledCode)
        runGeneratedSource()
    }
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
//    compileFromSource()
    compileFromSource("C:\\Users\\moham\\Desktop\\compilation\\MomidCompilation", "source")
}
