package com.momid.compiler

fun main(args: Array<String>) {

//    println("")
//
//    val generatedOutput = wholeProgram("printf(\"%d\\n\", 3 + 7 + 7);")
//
//    createSource(generatedOutput)
//
//    runGeneratedSource()

    val codeText = readMomidFileContents(args[0])
    val compiledCode = compile(codeText)
    println(compiledCode)
    createSource(compiledCode)
    runGeneratedSource()
}
