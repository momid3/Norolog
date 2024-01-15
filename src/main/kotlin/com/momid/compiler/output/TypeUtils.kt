package com.momid.compiler.output

fun referenceCType(type: Type): Type {
    return CReferenceType(type)
}
