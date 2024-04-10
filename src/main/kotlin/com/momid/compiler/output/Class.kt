package com.momid.compiler.output

import com.momid.compiler.forEveryIndexed

open class Class(val name: String, var variables: List<ClassVariable>, val declarationPackage: String = "") {

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

class GenericTypeParameter(var name: String, var substitutionType: OutputType? = null) {

    fun clone(): GenericTypeParameter {
        return GenericTypeParameter(this.name, null)
    }
}

open class GenericClass(name: String, variables: List<ClassVariable>, declarationPackage: String, val typeParameters: MutableList<GenericTypeParameter>, var unsubstituted: Boolean = true): Class(name, variables, declarationPackage) {

    override fun equals(other: Any?): Boolean {
        return other is GenericClass && other.name == this.name && other.declarationPackage == this.declarationPackage
                && other.typeParameters.forEveryIndexed { index, genericTypeParameter ->
                    genericTypeParameter.substitutionType == this.typeParameters[index].substitutionType
                            && other.unsubstituted == this.unsubstituted
        }
    }

    override fun clone(): GenericClass {
        val clonedTypeParameters = typeParameters.map { it.clone() } as MutableList
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
            return ClassType(GenericClass(genericClass.name, genericClass.variables.map {
                                                                                        ClassVariable(it.name, cloneOutputType(it.type, typeParameters))
            }, genericClass.declarationPackage, genericClass.typeParameters.map {
                if (it.substitutionType == null) {
                    (cloneOutputType(TypeParameterType(it), typeParameters) as TypeParameterType).genericTypeParameter
                } else {
                    val clonedSubstitution = cloneOutputType(it.substitutionType!!, typeParameters)
                    val clonedGenericTypeParameter = (cloneOutputType(TypeParameterType(it), typeParameters) as TypeParameterType).genericTypeParameter
                    clonedGenericTypeParameter.substitutionType = clonedSubstitution
                    clonedGenericTypeParameter
                }
            } as MutableList, genericClass.unsubstituted))
        }
    } else if (outputType is TypeParameterType) {
        val correspondingTypeParameter = typeParameters.find { it.name == outputType.genericTypeParameter.name }
        if (correspondingTypeParameter != null) {
            return TypeParameterType(correspondingTypeParameter)
        } else {
            throw (Throwable("type parameters should contain it" + outputType.genericTypeParameter.name))
        }
    } else if (outputType is ReferenceType) {
        return ReferenceType(cloneOutputType(outputType.actualType, typeParameters), outputType.underlyingCReferenceName)
    } else if (outputType is FunctionType) {
        val function = outputType.outputFunction
        if (function is ClassFunction) {
            return FunctionType(
                ClassFunction(
                    cloneOutputType(function.receiverType, typeParameters),
                    Function(
                        function.name,
                        function.parameters.map { FunctionParameter(it.name, cloneOutputType(it.type, typeParameters)) },
                        cloneOutputType(function.returnType),
                        function.bodyRange,
                        function.discover
                    )
                ),
                outputType.cFunction
            )
        } else {
            return FunctionType(
                Function(
                    function.name,
                    function.parameters.map { FunctionParameter(it.name, cloneOutputType(it.type, typeParameters)) },
                    cloneOutputType(function.returnType),
                    function.bodyRange,
                    function.discover
                ),
                outputType.cFunction
            )
        }
    }

    return outputType
}

fun cloneOutputType(outputType: OutputType): OutputType {
    if (outputType is ClassType) {
        if (outputType.outputClass is GenericClass) {
            val genericClass = outputType.outputClass
            return ClassType(GenericClass(genericClass.name, genericClass.variables, genericClass.declarationPackage, genericClass.typeParameters.map {
                (cloneOutputType(it.substitutionType!!) as TypeParameterType).genericTypeParameter
            } as MutableList, genericClass.unsubstituted))
        }
    } else if (outputType is TypeParameterType) {
        return TypeParameterType(outputType.genericTypeParameter.clone())
    } else if (outputType is ReferenceType) {
        return ReferenceType(cloneOutputType(outputType.actualType), outputType.underlyingCReferenceName)
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

val listClass = GenericClass("List", listOf(ClassVariable("size", outputIntType)), "norolog", mutableListOf(GenericTypeParameter("T")))

val window = Class("Window", listOf(), "norolog.graphics")
val renderer = Class("Renderer", listOf(), "norolog.graphics")
//val arrayClass = GenericClass("Array", arrayListOf(), "",  listOf(GenericTypeParameter("T", null, null))).apply {
//    this.typeParameters.forEach {
//        it.owningClass = this
//    }
//}
