package com.momid.compiler

import com.momid.parser.expression.get
import com.momid.parser.expression.insideOf
import com.momid.parser.expression.plus
import com.momid.parser.expression.spaces

val classFunctionCall by lazy {
    functionName["functionName"] + insideOf('(', ')') {
        functionCallParameters["functionParameters"]
    } + spaces
}
