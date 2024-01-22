package com.momid.compiler

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.pathString

fun runGeneratedSource() {
    val cCodeFile = File(directory, fileName)

//    val compileProcess = ProcessBuilder("gcc", "output.c", "-o", "output.exe").directory(File(directory)).redirectErrorStream(true).start()

    copyFromResources("CMakeLists.txt", directory)

    val buildDirectory = Path(directory).resolve("build").toFile()
    if (!buildDirectory.exists()) {
        buildDirectory.mkdir()
    }

    val cMakeProcess = ProcessBuilder("cmake", "-G", "\"MinGW Makefiles\"", Path(directory).pathString).directory(buildDirectory).redirectErrorStream(true).start()
    val cMakeOutput = cMakeProcess.inputStream.bufferedReader().readText()
    val cMakeExitCode = cMakeProcess.waitFor()

    if (cMakeExitCode == 0) {
        println("CMake successful")
        println(cMakeOutput)
    } else {
        println("CMake failed with error:")
        println(cMakeOutput)
    }

    val compileProcess = ProcessBuilder("mingw32-make").directory(buildDirectory).redirectErrorStream(true).start()

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

fun copyFromResources(fileName: String, destinationDirectory: String) {
    // Get the path of the resource file
    val resourcePath = FileSystems.getDefault().getPath("src/main/resources", fileName)

    // Specify the destination directory
    val destinationPath: Path = FileSystems.getDefault().getPath(destinationDirectory, fileName)

    try {
        // Copy the file using Files.copy method
        Files.copy(resourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
        println("File copied successfully.")
    } catch (e: Exception) {
        println("Error copying file: ${e.message}")
    }
}
