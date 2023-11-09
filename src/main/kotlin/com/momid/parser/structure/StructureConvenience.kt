package com.momid.parser.structure

//operator fun Structure.get(name: String): Expression {
//    return this.getExpression()[name]
//}

//operator fun Structure.plus(expression: Expression): Expression {
//    return this.getExpression() + expression
//}

//operator fun Expression.plus(structure: Structure): Expression {
//    return this + structure.getExpression()
//}
//
//inline operator fun <reified T : Structure> Unit.invoke(name: String): Expression {
//    val instance = T::class.createInstance()
//    return (instance as Structure).getExpression()[name]
//}
//
//inline operator fun <reified T : Structure> String.invoke(): Expression {
//    val instance = T::class.createInstance()
//    return (instance as Structure).getExpression()[this]
//}
//
//inline operator fun <reified T : Structure> T.invoke(name: String): Expression {
//    val instance = T::class.createInstance()
//    return (instance as Structure).getExpression()[name]
//}
