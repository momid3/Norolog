package com.momid.compiler

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.pathString

fun runGeneratedSource() {
    val cCodeFile = File(directory, fileName)

//    val compileProcess = ProcessBuilder("gcc", "output.c", "-o", "output.exe").directory(File(directory)).redirectErrorStream(true).start()

    copyFromResources("CMakeLists.txt", directory)
    copyFromResources("SDL", directory)

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

//    if (destinationPath.toFile().exists()) {
        try {
            destinationPath.toFile().deleteRecursively()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
//    }

    try {
        // Copy the file using Files.copy method
        copyRecursively(resourcePath, destinationPath)
    } catch (e: Exception) {
        println("Error copying file: ${e.message}")
    }
}

fun copyRecursively(source: Path, destination: Path) {
    Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val targetDir = destination.resolve(source.relativize(dir))
            Files.createDirectories(targetDir)
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
            return FileVisitResult.CONTINUE
        }
    })
}
