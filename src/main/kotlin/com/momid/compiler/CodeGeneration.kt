package com.momid.compiler

fun wholeProgram(programText: String, classDeclarations: String, functionDeclarations: String): String {
    return "#include <stdio.h>\n" +
            "\n" +
            classDeclarations +
            "\n" +
            functionDeclarations +
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
            " {" + "\n" + functionBody + "\n" + "}"
}

@JvmName("cStruct1")
fun cStruct(name: String, variablesNat: List<String>): String {
    return "struct " + name + " {" + "\n" +
            variablesNat.joinToString(";\n") +
            ";\n" + "}" + ";"
}
