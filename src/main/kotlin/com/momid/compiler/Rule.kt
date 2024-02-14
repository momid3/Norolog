//package com.momid.compiler
//
//import com.momid.compiler.output.Rule
//import com.momid.compiler.standard_library.continueComplexExpression
//import com.momid.parser.expression.*
//import com.momid.parser.not
//
//val ruleItem =
//    infoAccess + anyOf(!"=", !">", !"<") + complexExpression
//
//val rule =
//    !"rule" + space + splitByNW(ruleItem["ruleItem"], ",")["ifs"] + spaces + "->" + spaces + splitByNW(ruleItem["ruleItem"], ",")["thens"]
//
//fun ExpressionResultsHandlerContext.handleRule(currentGeneration: CurrentGeneration, rule: Rule): Result<Boolean> {
//    continueComplexExpression(
//        """
//            class()
//        """.trimIndent(),
//        currentGeneration
//    ) {
//
//    }
//}
