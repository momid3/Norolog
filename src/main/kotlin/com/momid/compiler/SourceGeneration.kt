package com.momid.compiler

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

val directory = "C:\\Users\\moham\\Desktop\\compilation"
val fileName = "output.c"

fun createSource(sourceText: String) {

    val directory = File(directory)
    if (!directory.exists()) {
        directory.mkdirs()
    }

    val file = File(directory, fileName)

    try {
        val writer = BufferedWriter(FileWriter(file))
        writer.write(sourceText)
        writer.close()

        println("generated source written to: $file")
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
    }
}
