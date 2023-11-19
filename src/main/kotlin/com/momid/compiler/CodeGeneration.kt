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

fun forLoop(indexName: String, rangeStart: String, rangeEnd: String, codeBlock: String): String {
    return "for (int " + indexName + " = " + rangeStart + "; " + indexName + " < " + rangeEnd + "; " + indexName + " += 1)" + " {" +
            codeBlock +
            "}"
}

fun cStruct(name: String, variables: List<Pair<String, String>>): String {
    return "struct " + name + " {" + "\n" +
            variables.joinToString(";\n") { it.second + " " + it.first } +
            "\n" + "}" + ";"
}
