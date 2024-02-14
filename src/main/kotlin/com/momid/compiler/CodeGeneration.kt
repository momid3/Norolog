package com.momid.compiler

fun wholeProgram(programText: String, classDeclarations: String, functionDeclarations: String): String {
    return "#include <stdio.h>\n" +
            "#include <unistd.h>\n" +
            "#include <stdbool.h>\n" +
            "#include <SDL2/SDL.h>\n" +
            "\n" +
            classDeclarations +
            "\n" +
            functionDeclarations +
            "\n" +
            "int main(int argc, char *argv[]) {\n" +
            indent(programText) +
            "\n" +
            indent("return 0;") +
            "\n" +
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

/***
 * @param structName name of the structure that is being initialized.
 * @param parameters list of structure parameters. first pair element
 * is the name of the structure parameter and the last one is its value.
 */
fun cStructInitialization(structName: String, parameters: List<Pair<String, String>>): String {
    return "(struct " + structName + ")" + " {" +
            parameters.joinToString(", ") { "." + it.first + " = " + it.second } +
            " }"
}

/***
 * @param variableType type of the variable that is being allocated.
 * it should be 'int' or 'bool' or 'struct SomeStruct' for structs or
 * 'struct SomeStruct *' for pointers
 */
fun memoryAllocation(variableName: String, variableType: String): String {
    return variableType + "*" + " " + variableName + " = " + "(" + variableType + "*" + ") " + "malloc(sizeof(" + variableType + "));"
}

fun memoryCopy(destination: String, source: String, sourceType: String): String {
    return "memcpy(" + destination + ", " + "&(" + source + ")" + ", " + "sizeof(" + sourceType + "));"
}

fun variableDeclaration(variableName: String, variableType: String, variableValue: String): String {
    return variableType + " " + variableName + " = " + variableValue + ";"
}

fun pointerDereference(pointerVariableName: String): String {
    return "*" + "(" + pointerVariableName + ")"
}

fun cFunction(name: String, parameters: List<Pair<String, String>>, returnType: String, functionBody: String): String {
    return returnType + " " + name + "(" + parameters.joinToString(", ") { it.second + " " + it.first } + ")" +
            " {" + "\n" + functionBody + "\n" + "}"
}

fun cFunctionCall(name: String, parameters: List<String>): String {
    return name + "(" + parameters.joinToString(", ") + ")"
}

fun returnStatement(returnExpression: String): String {
    return "return " + returnExpression + ";"
}

fun arrayAccess(array: String, index: String): String {
    return array + "[" + index + "]"
}

fun arrayInitialization(initializeWith: String, arraySize: Int): String {
    return "{" + "[" + "0" + " ... " + arraySize + "]" + " = " + initializeWith + "}"
}

fun variableDeclaration(variableTypeAndName: String, variableValue: String): String {
    return variableTypeAndName + " = " + variableValue + ";"
}

/***
 * variable declaration without initialization
 */
fun variableDeclaration(variableTypeAndName: String): String {
    return variableTypeAndName + ";"
}

/***
 * @param referenceTypeAndVariableName the type should be reference applied to the desired type to be allocated such as:
 * "CReference(TypeToBeAllocated)".
 * @param typeName the type that has to be allocated (without reference applied to it)
 * @param referenceTypeName the type that has to be allocated, with reference applied to it
 *
 * to get the name of the type and the "type and variable" use cTypeAndVariableName() and cTypeName()
 * functions.
 * @see cTypeAndVariableName
 * @see cTypeName
 */
fun memoryAllocate(referenceTypeAndVariableName: String, typeName: String, referenceTypeName: String): String {
    return referenceTypeAndVariableName + " = " + "(" + referenceTypeName + ") " + "malloc(sizeof(" + typeName + "));"
}

/***
 * @param parametersNat the type and name of each parameter altogether
 */
@JvmName("cFunction1")
fun cFunction(name: String, parametersNat: List<String>, returnType: String, functionBody: String): String {
    return returnType + " " + name + "(" + parametersNat.joinToString(", ") + ")" +
            " {" + "\n" + indent(functionBody) + "\n" + "}"
}

@JvmName("cStruct1")
fun cStruct(name: String, variablesNat: List<String>): String {
    return "struct " + name + " {" + "\n" +
            indent(variablesNat.joinToString(";\n")) +
            ";\n" + "}" + ";"
}

fun assignment(variable: String, value: String): String {
    return variable + " = " + value + ";"
}

fun propertyAccess(variableName: String, propertyName: String): String {
    return variableName + "." + propertyName
}

/***
 * @param referenceTypeAndVariableName the type should be reference applied to the desired type to be allocated such as:
 * "CReference(TypeToBeAllocated)".
 * @param typeName the type that has to be allocated (without reference applied to it)
 * @param referenceTypeName the type that has to be allocated, with reference applied to it
 *
 * to get the name of the type and the "type and variable" use cTypeAndVariableName() and cTypeName()
 * functions.
 * @see cTypeAndVariableName
 * @see cTypeName
 */
fun memoryAllocateList(referenceTypeAndVariableName: String, typeName: String, referenceTypeName: String, listSize: Int): String {
    return referenceTypeAndVariableName + " = " + "(" + referenceTypeName + ") " + "malloc(" + listSize + " * sizeof(" + typeName + "));"
}

fun indent(text: String, indentSize: Int = 4): String {
    return text.lines().joinToString("\n") { (0 until indentSize).fold("") { acc, i -> acc + " " } + it }
}
