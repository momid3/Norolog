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

val listSetFunctionText = """
    |fun <T> List<T>.set(item: T) {
    |
    |}
""".trimIndent()

var listSetFunction: GenericFunction? = null
fun listSetFunction(currentGeneration: CurrentGeneration) {
//    listSetFunction = continueGiven(
//        listSetFunctionText,
//        classFunction
//    ) {
//        handleClassFunction(currentGeneration)
//    }.okOrReport {
//        throw (Throwable("list set function has issues " + it.error))
//    } as GenericFunction
}
