package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.compiler.terminal.printError
import com.momid.parser.expression.*
import com.momid.parser.not

val classVariableExp by lazy {
    variableNameO["variableName"] + spaces + ":" + spaces + outputTypeO["variableType"]
}

val classVariable by lazy {
    ignoreParentheses(condition { it != ',' && it != '}' })
}

val classVariables by lazy {
    spaces + inline(classVariable["classVariable"] + inline(some0(spaces + !"," + spaces + classVariable["classVariable"])) + spaces) + spaces
}

val klass by lazy {
    !"class" + space + variableNameO["className"] + spaces + insideOf("classInside", '{', '}')
}

fun ExpressionResultsHandlerContext.handleClass(currentGeneration: CurrentGeneration): Result<String> {
    this.expressionResult.isOf(klass) {
        val classVariablesOutput = ArrayList<ClassVariable>()

        val className = it["className"].tokens()

        val outputClass = Class(className, classVariablesOutput)

        currentGeneration.classesInformation.classes[outputClass] = null

        val classVariablesEvaluation = continueWithOne(it["classInside"].content, classVariables) { handleClassVariables(currentGeneration) }

        classVariablesEvaluation.handle({
            println(it.error)
            return it.to()
        }, {
            it.forEach {
                if (it.type == ClassType(outputClass)) {
                    printError("a class cannot have a variable of its own type. instead you can have a reference of that type")
                    return Error("a class cannot have a variable of its own type. instead you can have a reference of that type", this.expressionResult.range)
                }
                val classVariable = ClassVariable(it.name, it.type)
                classVariablesOutput.add(classVariable)
            }

            val outputStruct = CStruct(currentGeneration.createCStructName(), classVariablesOutput.map {
                CStructVariable(it.name, resolveType(it.type, currentGeneration))
            })

            currentGeneration.classesInformation.classes[outputClass] = outputStruct

            currentGeneration.globalDefinitionsGeneratedSource += cStruct(outputStruct.name, outputStruct.variables.map { Pair(it.name, it.type.name) }) + "\n"

            return Ok("")
        })
    }
    println("is not class")
    return Error("is not class", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleClassVariables(currentGeneration: CurrentGeneration): Result<List<ClassVariableEvaluation>> {
    this.expressionResult.isOf(classVariables) {
        var hasErrors = false
        val classVariables = ArrayList<ClassVariableEvaluation>()
        it.forEach {
            it.forEach {
                val classVariable = continueWithOne(it, classVariableExp) { handleClassVariable(currentGeneration) }

                classVariable.handle({
                    currentGeneration.errors.add(it)
                }, {
                    classVariables.add(it)
                })
            }
        }
        return Ok(classVariables)
    }
    println("is not class variables")
    return Error("is not class variables", this.expressionResult.range)
}

fun ExpressionResultsHandlerContext.handleClassVariable(currentGeneration: CurrentGeneration): Result<ClassVariableEvaluation> {
    this.expressionResult.isOf(classVariableExp) {
        val name = it["variableName"].tokens()
        val type = continueStraight(it["variableType"]) { handleOutputType(currentGeneration) }.okOrReport {
            println(it.error)
            return it.to()
        }
        return Ok(ClassVariableEvaluation(name, type))
    }
    return Error("is not class variable", this.expressionResult.range)
}

fun resolveClass(outputType: Class, currentGeneration: CurrentGeneration): CStruct {
    return currentGeneration.classesInformation.classes.entries.find {
        it.key == outputType && if (it.key is GenericClass) {
            !(it.key as GenericClass).unsubstituted
        } else {
            true
        }
    }?.value ?: throw (Throwable("could not resolve corresponding CStruct to this class: " + outputType))
}

fun resolveType(outputType: Class, currentGeneration: CurrentGeneration): Type {
    if (outputType == outputInt) {
        return Type.Int
    } else if (outputType == outputString) {
        return Type.CharArray
    } else if (outputType == outputBoolean) {
        return Type.Boolean
    } else {
        return Type(resolveClass(outputType, currentGeneration).name)
    }
}

fun resolveType(outputType: OutputType, currentGeneration: CurrentGeneration): Type {
    if (outputType is ClassType) {
        return resolveType(outputType.outputClass, currentGeneration)
    } else {
        if (outputType is TypeParameterType) {
            val substitution = outputType.genericTypeParameter.substitutionType ?:
            throw (Throwable("type variable " + outputType.genericTypeParameter.name + " should have been substituted with a type"))
            return resolveType(substitution, currentGeneration)
        } else if (outputType is ReferenceType) {
            return CReferenceType(resolveType(outputType.actualType, currentGeneration))
        } else if (outputType is ArrayType) {
            return CArrayType(resolveType(outputType.itemsType, currentGeneration), outputType.size)
        } else if (outputType is NorType) {
            return Type.Void
        } else {
            throw (Throwable("only class type is available currently"))
        }
    }
}

fun confirmTypeIsClassType(outputType: OutputType): ClassType {
    if (outputType is ClassType) {
        return outputType
    } else {
        throw (Throwable("only class type is available currently"))
    }
}

fun resolveType(outputTypeName: String, currentGeneration: CurrentGeneration): Class? {
    val outputClass = currentGeneration.classesInformation.classes.entries.find {
        it.key.name == outputTypeName && if (it.key is GenericClass) {
            (it.key as GenericClass).unsubstituted
        } else {
            true
        }
    }?.key.also {
        if (it == null) {
            println("class with this name not found: " + outputTypeName)
        }
    }
    if (outputClass is GenericClass) {
        return outputClass.clone()
    } else {
        return outputClass
    }
}

/***
 * @return the output of this c type that can be used or inserted to the output
 */
fun cTypeName(type: Type): String {
    return cTypeAndVariableName(type, "")
}

fun cTypeName(outputType: OutputType, currentGeneration: CurrentGeneration): String {
    val cType = resolveType(outputType, currentGeneration)
    return cTypeName(cType)
}

fun cTypeAndVariableName(type: Type, variableName: String): String {
    val (base, tail) = cTypeAndVariableNameBase(type, variableName)
    return base + " " + tail
}

/***
 * returns the full type representation of this type. such as:
 * "int * someVariable" or "int (*someVariable)[3]"
 * @return the representation as Pair of (base, tail) which should be appended
 */
private fun cTypeAndVariableNameBase(type: Type, variableName: String, currentTail: String = variableName): Pair<String, String> {
    when (type) {
        Type.Int -> {
            return Pair(type.name, currentTail)
        }
        Type.Boolean -> {
            return Pair(type.name, currentTail)
        }
        Type.Void -> {
            return Pair(type.name, currentTail)
        }
        is CReferenceType -> {
            val shouldHaveParentheses = type.actualType
            val modifiedTail = "(" + "*" + currentTail + ")"
            return cTypeAndVariableNameBase(type.actualType, variableName, modifiedTail)
        }
        is CArrayType -> {
            val modifiedTail = currentTail + "[" + type.size + "]"
            return cTypeAndVariableNameBase(type.itemsType, variableName, modifiedTail)
        }
        else -> {
            return Pair("struct " + type.name, currentTail)
        }
    }
}

fun cTypeAndVariableName(outputType: OutputType, variableName: String, currentGeneration: CurrentGeneration): String {
    val cType = resolveType(outputType, currentGeneration)
    return cTypeAndVariableName(cType, variableName)
}

fun resolveOutputType(outputTypeName: String, currentGeneration: CurrentGeneration): OutputType? {
    val genericParameter = resolveGenericParameter(outputTypeName, currentGeneration)
    if (genericParameter != null) {
        return genericParameter
    }

    val outputClass = resolveType(outputTypeName, currentGeneration)
    if (outputClass != null) {
        return ClassType(outputClass)
    }

    return null
}

fun resolveGenericParameter(genericParameterName: String, currentGeneration: CurrentGeneration): TypeParameterType? {
    var currentScope = currentGeneration.currentScope
    while (true) {
        val scopeContext = currentScope.scopeContext
        when (scopeContext) {
            is ClassContext -> {
                if (scopeContext.outputClass is GenericClass) {
                    scopeContext.outputClass.typeParameters.forEach {
                        if (it.name == genericParameterName) {
                            return TypeParameterType(it)
                        }
                    }
                }
            }

            is FunctionContext -> {

            }
        }

        currentScope = currentScope.upperScope ?: return null
    }
}

inline fun <T> Result<T>.handle(error: (Error<T>) -> Unit = {  }, ok: (T) -> Unit) {
    if (this is Ok) {
        ok(this.ok)
    }

    if (this is Error) {
        error(this)
    }
}

class ClassVariableS(val name: String, val type: String)

class ClassVariableEvaluation(val name: String, val type: OutputType)

fun main() {
//    val currentGeneration = CurrentGeneration()
//    val text = "class SomeClass { someVariable: SomeType, anotherVariable: AnotherType, someOtherVariable: SomeOtherType }".toList()
//    val finder = ExpressionFinder()
//    finder.registerExpressions(listOf(klass))
//    finder.start(text).forEach {
//        handleExpressionResult(finder, it, text) {
//            handleClass(currentGeneration)
//        }
//    }

    println(cTypeAndVariableName(CReferenceType(CArrayType(Type.Int, 3)), "someVariable"))
}
