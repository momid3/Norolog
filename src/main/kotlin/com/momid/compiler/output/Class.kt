package com.momid.compiler.output

class Class(val name: String, val variables: List<ClassVariable>, val definitionPackage: String = "")

class ClassVariable(val name: String, val type: OutputType)

class CStruct(val name: String, val variables: List<CStructVariable>)

class CStructVariable(val name: String, val type: Type)

class ClassesInformation(val classes: HashMap<Class, CStruct> = HashMap())

val outputInt = Class("Int", arrayListOf())

val outputString = Class("String", arrayListOf())

val outputNothing = Class("Nothing", arrayListOf())
