package com.momid.compiler

import com.momid.parser.expression.*
import com.momid.parser.not

val space =
    some(condition { it.isWhitespace() })

val assignment =
    !"val" + space + variableName["variableName"] + spaces + "=" + spaces + complexExpression["assignmentExpression"]


fun ExpressionResultsHandlerContext.handleAssignment() {

}
