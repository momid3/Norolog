package com.momid.compiler.output

import com.momid.compiler.CurrentGeneration
import com.momid.compiler.resolveType

class ListClass(listClass: GenericClass): GenericClass(
    listClass.name,
    listClass.variables,
    listClass.declarationPackage,
    listClass.typeParameters,
    listClass.unsubstituted
)

fun createCListStruct(type: OutputType, size: Int, currentGeneration: CurrentGeneration): CStruct {
    return CStruct(
        currentGeneration.createCStructName("CList"),
        listOf(
            CStructVariable("list_pointer", CReferenceType(resolveType(type, currentGeneration))),
            CStructVariable("size", Type.Int)
        )
    )
}
