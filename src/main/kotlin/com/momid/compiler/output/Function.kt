package com.momid.compiler.output

/***
 * class representing the definition of a function.
 * @param name name of the function.
 * @param parameters parameters of the function.
 * @param returnType return type of the function.
 * @param bodyRange index range of the body of the function in the source code.
 */
class Function(val name: String, val parameters: List<FunctionParameter>, val returnType: OutputType, val bodyRange: IntRange) {
}

class FunctionParameter(val name: String, val type: OutputType, relatedCFunction: CFunction? = null)

class CFunction(val name: String, val parameters: List<CFunctionParameter>, val returnType: Type, bodyText: String)

class CFunctionParameter(val name: String, val type: Type)

class FunctionsInformation(val functionsInformation: HashMap<Function, CFunction>)
