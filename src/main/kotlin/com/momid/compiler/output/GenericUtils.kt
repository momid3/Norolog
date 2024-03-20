package com.momid.compiler.output

import com.momid.compiler.CurrentGeneration
import com.momid.compiler.cStruct
import com.momid.compiler.cTypeName
import com.momid.compiler.resolveType

fun createGenericClassIfNotExists(currentGeneration: CurrentGeneration, genericClass: GenericClass): CStruct {
    val alreadyExists = currentGeneration.classesInformation.classes.entries.find {
        it.key == genericClass
    } != null

    val cStructVariables = ArrayList<CStructVariable>()
    var cStruct = CStruct(currentGeneration.createCStructName(), cStructVariables)

    genericClass.variables.forEach {
        cStructVariables.add(CStructVariable(it.name, resolveType(it.type, currentGeneration)))
    }

    if (alreadyExists) {
        cStruct = currentGeneration.classesInformation.classes.entries.find {
            it.key == genericClass
        }!!.value!!
    } else {
        currentGeneration.classesInformation.classes[genericClass] = cStruct

        currentGeneration.globalDefinitionsGeneratedSource += cStruct(cStruct.name, cStruct.variables.map { Pair(it.name, cTypeName(it.type)) })
    }

    return cStruct
}
