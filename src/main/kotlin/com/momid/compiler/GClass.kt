package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val classDeclarationParameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val cdp = classDeclarationParameter

val classDeclarationParameters =
    oneOrZero(
        splitBy(cdp, ",")
    )

val typeParameters =
    splitBy(className, ",")

val gClass =
    !"class" + space + oneOrZero(insideOf('<', '>') {
        typeParameters["typeParameters"]
    }, "typeParameters") + spaces + className["className"] + insideOf('(', ')') {
        classDeclarationParameters["classDeclarationParameters"]
    }

fun ExpressionResultsHandlerContext.handleClassDeclarationParsing(): Result<ClassPE> {
    with(this.expressionResult) {
        val className = this["className"]
        val classNameText = className.tokens()
        val classParameters = this["classDeclarationParameters"].continuing?.continuing?.asMulti()?.map {
            println(it.tokens())
            val cdp = it.continuing {
                return Error("expected class parameter, found: " + it.tokens(), it.range)
            }
            val parameterName = cdp["parameterName"]
            val parameterType = cdp["parameterType"]
            ClassParameterPE(parsing(parameterName), parsing(parameterType))
        }

        val typeParameters = this["typeParameters"].continuing?.continuing?.asMulti()?.map {
            println("type parameter: " + it.tokens())
            val typeParameter = it.also { println(it::class) }.continuing {
                return Error("expected class parameter, found: " + it.tokens(), it.range)
            }
            ClassTypeVariablePE(parsing(typeParameter))
        }

        val name = parsing(className)
        return Ok(ClassPE(name, classParameters.orEmpty(), typeParameters.orEmpty()))
    }
}

fun ExpressionResultsHandlerContext.handleClassDeclaration(currentGeneration: CurrentGeneration): Result<Boolean> {
    val classDeclarationPE = continueStraight(this.expressionResult) { handleClassDeclarationParsing() }.okOrReport {
        return it.to()
    }

    val classParameters = ArrayList<ClassVariable>()
    val classTypeVariables = ArrayList<GenericTypeParameter>()

    val isGenericClass = classDeclarationPE.typeVariables.isNotEmpty()

    val outputClass = if (isGenericClass) {
        GenericClass(classDeclarationPE.name.tokens, classParameters, "", classTypeVariables)
    } else {
        Class(classDeclarationPE.name.tokens, classParameters)
    }

    if (isGenericClass) {
        classDeclarationPE.typeVariables.forEach {
            classTypeVariables.add(GenericTypeParameter(it.name.tokens, null))
        }
    }

    val classScope = Scope()
    classScope.scopeContext = ClassContext(outputClass)
    currentGeneration.createScope(classScope)

    if (isGenericClass) {
        currentGeneration.classesInformation.classes[outputClass] = null

        classDeclarationPE.parameters.forEach {
            val name = it.name.tokens
            val outputType = continueWithOne(it.outputTYpe.expressionResult, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }
            val classVariable = ClassVariable(name, outputType)
            classParameters.add(classVariable)
        }
    } else {
        val cStructVariables = ArrayList<CStructVariable>()
        val cStruct = CStruct(currentGeneration.createCStructName(), cStructVariables)
        currentGeneration.classesInformation.classes[outputClass] = cStruct

        classDeclarationPE.parameters.forEach {
            val name = it.name.tokens
            val outputType = continueWithOne(it.outputTYpe.expressionResult, outputTypeO) { handleOutputType(currentGeneration) }.okOrReport {
                return it.to()
            }
            val classVariable = ClassVariable(name, outputType)
            classParameters.add(classVariable)

            cStructVariables.add(CStructVariable(classVariable.name, resolveType(classVariable.type, currentGeneration)))
        }

        currentGeneration.globalDefinitionsGeneratedSource += cStruct(cStruct.name, cStruct.variables.map { cTypeAndVariableName(it.type, it.name) }) + "\n"
    }

    return Ok(true)
}

class ClassTypeVariablePE(val name: Parsing)

class ClassParameterPE(val name: Parsing, val outputTYpe: Parsing)

class ClassPE(val name: Parsing, val parameters: List<ClassParameterPE>, val typeVariables: List<ClassTypeVariablePE>)
