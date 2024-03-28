package com.momid.compiler.standard_library

import com.momid.compiler.*
import com.momid.compiler.output.*
import com.momid.parser.expression.Ok
import com.momid.parser.expression.Result

fun handleBuiltInClassInitialization(resolvedClass: Class, parametersEvaluation: List<String>, currentGeneration: CurrentGeneration): Result<Pair<String, OutputType>>? {
    if (resolvedClass.name == "List") {
        val listClass = resolvedClass as GenericClass
        val listType = resolvedClass.typeParameters[0].substitutionType!!
        val listSize = parametersEvaluation[0].toInt()
        val cStruct: CStruct
        if (genericClassAlreadyExists(listClass, currentGeneration)) {
            cStruct = findGenericClassIfAlreadyExists(listClass, currentGeneration)!!
        } else {
            cStruct = createCListStruct(listType, listSize, currentGeneration)

            currentGeneration.classesInformation.classes[listClass] = cStruct
            currentGeneration.globalDefinitionsGeneratedSource += cStruct(cStruct.name, cStruct.variables.map { cTypeAndVariableName(it.type, it.name) })
        }

        val listReferenceType = ReferenceType(listType, "")
        val cPointerName = "pointer_" + createVariableName()

        val cPointerTypeAndName = cTypeAndVariableName(listReferenceType, cPointerName, currentGeneration)
        val cType = cTypeName(listType, currentGeneration)
        val cPointerType = cTypeName(listReferenceType, currentGeneration)

        currentGeneration.currentScope.generatedSource += memoryAllocateList(cPointerTypeAndName, cType, cPointerType, listSize) + "\n"

        val output = cStructInitialization(cStruct.name, cStruct.variables.mapIndexed { index, cStructVariable ->
            Pair(
                cStructVariable.name, if (cStructVariable.name == "list_pointer") {
                    cPointerName
                } else if (cStructVariable.name == "size") {
                    "" + listSize
                } else {
                    ""
                }
            )
        })

        return Ok(Pair(output, ClassType(listClass)))
    }
    return null
}