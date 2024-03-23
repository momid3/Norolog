//package com.momid.parser
//
//import com.momid.parser.expression.*
//import com.momid.parser.structure.*
//
//class VariableName: Structure(
//    condition { it.isLetter() } + some0(condition { it.isLetterOrDigit() })
//)
//
//class Number: Structure(
//    condition { it.isDigit() } + some0(condition { it.isDigit() })
//)
//
//class AtomicExp: Continued(
//    anyOf(!VariableName(), !Number())
//)
//
//class Operator: Structure(
//    anyOf('+', '-', '*', '/')
//)
//
//val simpleExpression =
//    some(anyOf(spaces + !AtomicExp() + spaces, spaces + !Operator() + spaces))
//
//val simpleExpressionInParentheses =
//    exact("(") + simpleExpression + ")"
//
////val simpleExpressionInOrOutOfParentheses =
////    anyOf(!SimpleExpression(), !SimpleExpressionInParentheses())
//
////val complexExpression =
////    some(anyOf(!SimpleExpression(), !SimpleExpressionInParentheses(), !Operator()))
//
//class SimpleExpression: Structure(
////    simpleExpression
//    some(anyOf(spaces + !AtomicExp() + spaces, spaces + !Operator() + spaces))
//)
//
//class SimpleExpressionInParentheses(var inside: ExpressionInParentheses? = null): Structure(
//    !"(" + SimpleExpressionInParentheses::inside + ")"
//)
//
////class ComplexOperator: Structure(
////    simpleExpressionInOrOutOfParentheses + anyOf('+', '-', '*', '/') + simpleExpressionInOrOutOfParentheses
////)
//
//class ComplexExpression: Continued(
//    some(anyOf(!SimpleExpression(), !SimpleExpressionInParentheses())),
//)
//
//class ExpressionInParentheses: Continued(
//    insideParentheses
//)
//
//val dotInTheMiddleOfNumber = CustomExpression() { tokens, startIndex, endIndex ->
//    if (startIndex > 0 && tokens[startIndex - 1].isDigit() && startIndex < tokens.lastIndex && tokens[startIndex + 1].isDigit()) {
//        return@CustomExpression startIndex + 1
//    } else {
//        return@CustomExpression -1
//    }
//}
//
//operator fun Structure.not(): Expression {
//    println(this)
//    if (this is Continued) {
//        return this.continueWithExpression!!
//    } else {
//        return this.template.toExpression()!!
//    }
//
//}
//
//fun Template.toExpression(): Expression? {
//    if (this is Expression) {
//        return this
//    } else {
//        return null
//    }
//}
//
//fun main() {
//
//    val text = "someVar + 3 + 7".toList()
//    val finder = StructureFinder()
//    finder.registerStructures(ComplexExpression::class)
//    val structures = finder.start(text)
//    structures.forEach { structure ->
//        println(structure.correspondingTokens(text).joinToString(""))
//    }
//}
