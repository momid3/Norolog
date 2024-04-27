package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.packaging.FilePackage
import com.momid.compiler.packaging.readFilesRecursively
import com.momid.compiler.terminal.printError
import com.momid.parser.expression.ExpressionFinder
import com.momid.parser.expression.handleExpressionResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

fun compile(codeText: String, currentGeneration: CurrentGeneration, filePackage: FilePackage): String {
    val text = codeText.toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(statementsExp))

    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            handleStatements(currentGeneration, filePackage)
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
        currentGeneration.mainFunctionGeneratedSource,
        currentGeneration.globalDefinitionsGeneratedSource,
        currentGeneration.functionDeclarationsGeneratedSource
    )
}

fun compileFromSource(currentGeneration: CurrentGeneration) {
    val codeText = readResourceFileContents("source.momid")
    val compiledCode = compile(codeText, currentGeneration, FilePackage("", ""))
    println(compiledCode)
    createSource(compiledCode)
    runGeneratedSource()
}

fun compileFromSource(rootDirectory: String, mainFilePackage: FilePackage) {
    val currentGeneration = CurrentGeneration(rootDirectory, mainFilePackage).apply {
        this.classesInformation.classes[Class(outputInt.name, arrayListOf())] = CStruct(Type.Int.name, arrayListOf())
        this.classesInformation.classes[Class(outputString.name, arrayListOf())] = CStruct(Type.CharArray.name, arrayListOf())
        this.classesInformation.classes[Class(outputNorType.name, arrayListOf())] = CStruct(Type.Void.name, arrayListOf())
        this.classesInformation.classes[listClass] = null
        this.classesInformation.classes[renderer] = CStruct("SDL_Renderer", listOf())
    }

    listSetFunction(currentGeneration)

    readFilesRecursively(rootDirectory, mainFilePackage) { isMainFile, filePackage, fileContent ->
        val codeText = fileContent
        compile(codeText, currentGeneration, filePackage)
    }

    val compiledCode = wholeProgram(
        currentGeneration.mainFunctionGeneratedSource,
        currentGeneration.globalDefinitionsGeneratedSource,
        currentGeneration.functionDeclarationsGeneratedSource
    )

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
//    compileFromSource()
    compileFromSource("C:\\Users\\moham\\Desktop\\compilation\\MomidCompilation", FilePackage("", "source"))
}
