package com.momid.compiler

fun wholeProgram(programText: String): String {
    return "#include <stdio.h>\n" +
            "\n" +
            "int main() {\n" +
            programText +
            "\n" +
            "return 0;\n" +
            "}"
}
