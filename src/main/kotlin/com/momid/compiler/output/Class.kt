package com.momid.compiler.output

class Class(val name: String, val variables: List<ClassVariable>, val declarationPackage: String = "") {
    override fun equals(other: Any?): Boolean {
        return other is Class && other.name == this.name && other.declarationPackage == this.declarationPackage
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + variables.hashCode()
        result = 31 * result + declarationPackage.hashCode()
        return result
    }
}

class ClassVariable(val name: String, val type: OutputType)

class CStruct(val name: String, val variables: List<CStructVariable>)

class CStructVariable(val name: String, val type: Type)

class ClassesInformation(val classes: HashMap<Class, CStruct> = HashMap())

val outputInt = Class("Int", arrayListOf())

val outputString = Class("String", arrayListOf())

val outputNothing = Class("Nothing", arrayListOf())
