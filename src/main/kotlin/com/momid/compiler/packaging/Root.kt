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
    mainFilePackage: String,
    onEachFileContent: (isMainFile: Boolean, fileContent: String) -> Unit
) {

    val rootDirectory = File(rootPath)

    if (!rootDirectory.exists() || !rootDirectory.isDirectory) {
        throw (Throwable("invalid root path"))
    }

    val mainFile = fileFromPackage(rootDirectory, mainFilePackage) ?: throw (Throwable("provided package does not contain the main file or the package is malformed"))

    fun readFiles(directory: File) {
        val files = directory.listFiles()

        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    readFiles(file)
                } else {
                    val fileContent = file.readText()
                    val isMainFile = file == mainFile
                    onEachFileContent(isMainFile, fileContent)
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
