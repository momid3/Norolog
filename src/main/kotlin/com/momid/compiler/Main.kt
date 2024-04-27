package com.momid.compiler

import com.momid.compiler.packaging.FilePackage

fun main(args: Array<String>) {

//    println("")
//
//    val generatedOutput = wholeProgram("printf(\"%d\\n\", 3 + 7 + 7);")
//
//    createSource(generatedOutput)
//
//    runGeneratedSource()

    val currentGeneration = CurrentGeneration("", FilePackage("", ""))
    val codeText = readMomidFileContents(args[0])
    val compiledCode = compile(codeText, currentGeneration, FilePackage("", ""))
    println(compiledCode)
    createSource(compiledCode)
    runGeneratedSource()
}
