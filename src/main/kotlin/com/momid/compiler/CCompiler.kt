package com.momid.compiler

import java.io.File

fun runGeneratedSource() {
    val cCodeFile = File(directory, fileName)

    val compileProcess = ProcessBuilder("gcc", "output.c", "-o", "output.exe").directory(File(directory)).redirectErrorStream(true).start()

    val compileOutput = compileProcess.inputStream.bufferedReader().readText()

    val compileExitCode = compileProcess.waitFor()

    if (compileExitCode == 0) {
        println("compilation successful")

        val runProcess = ProcessBuilder(directory + "\\" + "output.exe").redirectErrorStream(true).directory(File(directory)).start()

        val output = runProcess.inputStream.bufferedReader().readText()
        println("output:")
        println(output)

        val runExitCode = runProcess.waitFor()
        println("program exited with code $runExitCode")
    } else {
        val errorOutput = compileProcess.errorStream.bufferedReader().readText()
        println("compilation failed with error:")
        println(compileOutput)
    }
}
