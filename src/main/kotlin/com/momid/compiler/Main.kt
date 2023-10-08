package com.momid.compiler

fun main() {

    println("")

    val generatedOutput = wholeProgram("printf(\"%d\\n\", 3 + 7 + 7);")

    createSource(generatedOutput)

    runGeneratedSource()
}
