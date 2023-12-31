package com.momid.compiler.output

import com.momid.compiler.forEveryIndexed

open class Class(val name: String, val variables: List<ClassVariable>, val declarationPackage: String = "") {

    override fun equals(other: Any?): Boolean {
        return other is Class && other.name == this.name && other.declarationPackage == this.declarationPackage
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + variables.hashCode()
        result = 31 * result + declarationPackage.hashCode()
        return result
    }

    open fun clone(): Class {
        return Class(this.name, variables.map { it.clone() }, this.declarationPackage)
    }
}

class ClassVariable(val name: String, val type: OutputType) {
    fun clone(): ClassVariable {
        return ClassVariable(this.name, this.type)
    }
}

class GenericTypeParameter(val name: String, var substitutionType: OutputType? = null) {
    fun clone(): GenericTypeParameter {
        return GenericTypeParameter(this.name)
    }
}

class GenericClass(name: String, variables: List<ClassVariable>, declarationPackage: String, val typeParameters: List<GenericTypeParameter>, var unsubstituted: Boolean = true): Class(name, variables, declarationPackage) {

    override fun equals(other: Any?): Boolean {
        return other is GenericClass && other.name == this.name && other.declarationPackage == this.declarationPackage
                && other.typeParameters.forEveryIndexed { index, genericTypeParameter ->
                    genericTypeParameter.substitutionType == this.typeParameters[index].substitutionType
                            && other.unsubstituted == this.unsubstituted
        }
    }

    override fun clone(): GenericClass {
        return GenericClass(this.name, variables.map { it.clone() }, declarationPackage, typeParameters.map { it.clone() })
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + typeParameters.hashCode()
        result = 31 * result + unsubstituted.hashCode()
        return result
    }
}

class CStruct(val name: String, val variables: List<CStructVariable>)

class CStructVariable(val name: String, val type: Type)

class ClassesInformation(val classes: HashMap<Class, CStruct?> = HashMap())

val outputInt = Class("Int", arrayListOf())

val outputString = Class("String", arrayListOf())

val outputNothing = Class("Nothing", arrayListOf())
