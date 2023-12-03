package com.momid.compiler.output

open class OutputType(val specifier: String = "") {
}

val nothingOutputType = NorType()
val outputIntType = ClassType(outputInt)
val outputStringType = ClassType(outputString)

open class Type(val name: String, val specifier: String = "") {
    companion object {
        val Int = Type("int")
        val Boolean = Type("bool")
        val CharArray = Type("CharArray")
    }
}

class CReferenceType(val actualType: Type): Type("")

class ClassType(val outputClass: Class) : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is ClassType && other.outputClass.name == this.outputClass.name
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

class NorType() : OutputType() {
    override fun equals(other: Any?): Boolean {
        return other is NorType
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
