package com.momid.compiler.packaging

import java.io.File

fun readFilesRecursively(rootPath: String, onEachFileContent: (fileContent: String) -> Unit) {

    fun readFiles(directory: File) {
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    readFiles(file)
                } else {
                    val fileContent = file.readText()
                    onEachFileContent(fileContent)
                }
            }
        }
    }

    val rootDirectory = File(rootPath)

    if (rootDirectory.exists() && rootDirectory.isDirectory) {
        readFiles(rootDirectory)
    } else {
        println("Invalid root path or not a directory.")
    }
}

/***
 * @param mainFilePackage the package of the main file along with the name of the main file without its extension postfix.
 * the package elements should each be a folder. it should be like this: "com.momid.someproject.TheMainFile".
 */
fun readFilesRecursively(
    rootPath: String,
    mainFilePackage: FilePackage,
    onEachFileContent: (isMainFile: Boolean, filePackage: FilePackage, fileContent: String) -> Unit
) {

    val rootDirectory = File(rootPath)

    if (!rootDirectory.exists() || !rootDirectory.isDirectory) {
        throw (Throwable("invalid root path"))
    }

    val mainFileDirectory = if (mainFilePackage.directoryPackage.isEmpty()) {
        ""
    } else {
        mainFilePackage.directoryPackage + ""
    }

    val mainFile = fileFromPackage(rootDirectory, mainFileDirectory + mainFilePackage.fileName) ?: throw (Throwable("provided package does not contain the main file or the package is malformed"))

    fun readFiles(directory: File, currentPackage: String = "") {
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    val filePackage = if (currentPackage.isEmpty()) {
                        ""
                    } else {
                        currentPackage + "."
                    }
                    readFiles(file, filePackage + file.name)
                } else {
                    val fileContent = file.readText()
                    val isMainFile = file == mainFile
                    onEachFileContent(isMainFile, FilePackage(currentPackage, file.nameWithoutExtension), fileContent)
                }
            }
        }
    }

    readFiles(rootDirectory)
}

fun File.subDirectory(subDirectoryName: String): File? {
    this.listFiles()?.forEach {
        if (it.nameWithoutExtension == subDirectoryName) {
            return it
        }
    }
    return null
}

fun fileFromPackage(rootFile: File, packageName: String): File? {
    var currentFile = rootFile
    val packageParts = packageName.split(",")
    packageParts.forEach {
        currentFile = currentFile.subDirectory(it) ?: return null
    }
    return currentFile
}

class FilePackage(val directoryPackage: String, val fileName: String)
