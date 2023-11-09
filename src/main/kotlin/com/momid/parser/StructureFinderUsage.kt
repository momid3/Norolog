package com.momid.parser

import com.momid.parser.expression.condition
import com.momid.parser.expression.insideOfParentheses
import com.momid.parser.expression.insideParentheses
import com.momid.parser.expression.some
import com.momid.parser.structure.Continued
import com.momid.parser.structure.Structure
import com.momid.parser.structure.StructureFinder
import com.momid.parser.structure.correspondingTokens

class While(var expression: Exp? = null, var codeBlock: CodeBlock? = null): Structure(
//    spaced {
//        !"while" + "(" + !While::expression + ")" + "{" + !While::codeBlock + "}"
//    }
)

class Exp : Structure(
    insideParentheses(some(condition { it != '3' }))
)

class CodeBlock : Continued(
    insideOfParentheses('{', '}')
)

val whileText = ("while (some() && true) {" +
        "while (someOther() && true) {" +
        "weAreInsideAnotherWhile()" +
        "}" +
        "weAreInsideWhile()" +
        "}").toList()

fun main() {

    val finder = StructureFinder()
    finder.registerStructures(While::class)

    val structures = finder.start(whileText, 0)

    structures.forEach {
        if (it is While) {
            println(it.codeBlock!!.correspondingTokens(whileText).joinToString(""))
            println(it.codeBlock!!.continuedStructures[0].correspondingTokens(whileText).joinToString(""))
        }
    }
}
