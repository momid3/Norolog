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
        return GenericTypeParameter(this.name, null)
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
        val clonedTypeParameters = typeParameters.map { it.clone() }
        val clonedVariables = variables.map {
            ClassVariable(it.name, cloneOutputType(it.type, clonedTypeParameters))
        }
        return GenericClass(this.name, clonedVariables, declarationPackage, clonedTypeParameters)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + typeParameters.hashCode()
        result = 31 * result + unsubstituted.hashCode()
        return result
    }
}

fun cloneOutputType(outputType: OutputType, typeParameters: List<GenericTypeParameter>): OutputType {
    if (outputType is ClassType) {
        if (outputType.outputClass is GenericClass) {
            val genericClass = outputType.outputClass
            return ClassType(GenericClass(genericClass.name, genericClass.variables, genericClass.declarationPackage, genericClass.typeParameters.map {
                (cloneOutputType(it.substitutionType!!, typeParameters) as TypeParameterType).genericTypeParameter
            }, genericClass.unsubstituted))
        }
    } else if (outputType is TypeParameterType) {
        val correspondingTypeParameter = typeParameters.find { it.name == outputType.genericTypeParameter.name }
        if (correspondingTypeParameter != null) {
            return TypeParameterType(correspondingTypeParameter)
        }
    } else if (outputType is ReferenceType) {
        return ReferenceType(cloneOutputType(outputType.actualType, typeParameters), outputType.underlyingCReferenceName)
    }

    return outputType
}

class CStruct(val name: String, val variables: List<CStructVariable>)

class CStructVariable(val name: String, val type: Type)

class ClassesInformation(val classes: HashMap<Class, CStruct?> = HashMap())

val outputInt = Class("Int", arrayListOf())
val outputString = Class("String", arrayListOf())
val outputNothing = Class("Nothing", arrayListOf())
val outputBoolean = Class("Boolean", listOf())

val window = Class("Window", listOf(), "norolog.graphics")
val renderer = Class("Renderer", listOf(), "norolog.graphics")
//val arrayClass = GenericClass("Array", arrayListOf(), "",  listOf(GenericTypeParameter("T", null, null))).apply {
//    this.typeParameters.forEach {
//        it.owningClass = this
//    }
//}
