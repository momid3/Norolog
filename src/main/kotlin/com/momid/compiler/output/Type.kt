package com.momid.compiler.output

open class OutputType(val specifier: String = "") {
}

val norType = NorType()
val outputIntType = ClassType(outputInt)
val outputStringType = ClassType(outputString)
//val outputArrayType = ClassType(arrayClass)

open class Type(val name: String, val declarationPackage: String = "") {
    companion object {
        val Int = Type("int")
        val Boolean = Type("bool")
        val CharArray = Type("CharArray")
        val Void = Type("void")
    }
}

class CReferenceType(val actualType: Type): Type("")

class CArrayType(val itemsType: Type, val size: Int): Type("")

class ClassType(val outputClass: Class) : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is ClassType && other.outputClass == this.outputClass
    }

    override fun hashCode(): Int {
        return outputClass.hashCode()
    }
}

class FunctionType(val outputFunction: Function) : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is FunctionType && other.outputFunction == this.outputFunction
    }

    override fun hashCode(): Int {
        return outputFunction.hashCode()
    }
}

class ReferenceType(val actualType: OutputType, val underlyingCReferenceName: String) : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is ReferenceType && other.actualType == this.actualType
    }

    override fun hashCode(): Int {
        return actualType.hashCode()
    }
}

class TypeParameterType(val genericTypeParameter: GenericTypeParameter): OutputType()

class ArrayType(val itemsType: OutputType, val size: Int): OutputType()

class NorType() : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is NorType
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
