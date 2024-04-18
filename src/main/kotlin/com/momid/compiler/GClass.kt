package com.momid.compiler

import com.momid.compiler.output.*
import com.momid.parser.expression.*
import com.momid.parser.not

val classDeclarationParameter =
    spaces + className["parameterName"] + spaces + !":" + spaces + outputTypeO["parameterType"] + spaces

val cdp = classDeclarationParameter

val classDeclarationParameters =
    oneOrZero(
        splitByNW(cdp, ",")
    )

val typeParameters =
    splitBy(one(spaces + className["typeParameterName"] + spaces), ",")

val gClass =
    !"class" + space + oneOrZero(insideOf('<', '>') {
        typeParameters
    }["typeParameters"], "typeParameters")["typeParameters"] + spaces + className["className"] + insideOf('(', ')') {
        classDeclarationParameters
    }["classDeclarationParameters"]

fun ExpressionResultsHandlerContext.handleClassDeclarationParsing(): Result<ClassPE> {
    with(this.expressionResult) {
        val className = this["className"]
        val classNameText = className.tokens()
        val classParameters = this["classDeclarationParameters"].continuing?.continuing?.asMulti()?.map {
            println(it.tokens())
            val cdp = it
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

fun ExpressionResultsHandlerContext.handleClassDeclaration(currentGeneration: CurrentGeneration, discover: Boolean = false): Result<Class> {
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

    outputClass.signature = true
    outputClass.discover = true

    val discovered = checkIfClassAlreadyExists(outputClass, currentGeneration).okOrReport {
        return it.to()
    }

    if (discovered != null) {
        return Ok(discovered)
    }

    currentGeneration.classesInformation.classes[outputClass] = null

    if (isGenericClass) {
        classDeclarationPE.typeVariables.forEach {
            classTypeVariables.add(GenericTypeParameter(it.name.tokens, null))
        }
    }

    val classScope = Scope()
    classScope.scopeContext = ClassContext(outputClass)
    currentGeneration.createScope(classScope)

    if (isGenericClass) {
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

    currentGeneration.goOutOfScope()

    outputClass.signature = false

    return Ok(outputClass)
}

class ClassTypeVariablePE(val name: Parsing)

class ClassParameterPE(val name: Parsing, val outputTYpe: Parsing)

class ClassPE(val name: Parsing, val parameters: List<ClassParameterPE>, val typeVariables: List<ClassTypeVariablePE>)

fun printAllClasses(currentGeneration: CurrentGeneration) {
    currentGeneration.classesInformation.classes.entries.forEach {
        if (it.key is GenericClass) {
            println(ClassType(it.key).text + " " + it.value?.name + " unsubstituted:" + (it.key as GenericClass).unsubstituted)
        } else {
            println(it.key.name + " " + it.value?.name)
        }
    }
}

fun ExpressionResultsHandlerContext.checkIfClassAlreadyExists(klass: Class, currentGeneration: CurrentGeneration): Result<Class?> {
    val existingClass = currentGeneration.classesInformation.classes.entries.find {
        val currentFunction = it.key
        (it.key == klass).also {
            println("expected class " + klass.name + " current class " + currentFunction.name + " " + it)
        }
    }
    if (existingClass != null && !existingClass.key.discover) {
        return Error("class is already declared " + this.expressionResult.tokens, this.expressionResult.range)
    }
    if (existingClass?.key?.discover == true) {
        return Ok(existingClass.key)
    } else {
        return Ok(null)
    }
}

fun main() {
    val currentGeneration = CurrentGeneration()
    val text = "class <T> someClass(someParameter: T)".toList()
    val finder = ExpressionFinder()
    finder.registerExpressions(listOf(gClass))
    finder.start(text).forEach {
        handleExpressionResult(finder, it, text) {
            println("found " + it.tokens)
            handleClassDeclaration(currentGeneration)
        }
    }
}
