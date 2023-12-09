package com.momid.compiler.terminal

val reset = "\u001B[0m"
val red = "\u001B[31m"
val green = "\u001B[32m"
val yellow = "\u001B[33m"
val blue = "\u001B[34m"

fun blue(text: String): String {
    return blue + text + reset
}

fun green(text: String): String {
    return green + text + reset
}

fun red(text: String): String {
    return red + text + reset
}

fun yellow(text: String): String {
    return yellow + text + reset
}

fun printError(text: String) {
    println(red(text))
}

fun main() {
    println(blue("something occurred"))
}
